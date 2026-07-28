/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.log.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.LongAdder

/**
 * 本地 buildId 窗口吞吐统计，用于热点构建队列分流。
 *
 * 不使用 Redis 行号键做吞吐：该键是构建全量累计值，且在消费写 ES 时才 INCR；
 * 入口决定队列投放时若再 GET/采样会额外增加 QPS，故按约定使用本地窗口。
 *
 * 说明：各 Pod 独立统计，跨 Pod 不完全一致；对「打到本实例的热点 build 分流」足够，
 * 且不增加 Redis 压力。开启 [routeHeavyEnabled] 后，窗口内超阈值的 build 投 heavy origin 队列。
 *
 * 计数窗口（[windowMs]）与热点标记（[heavyStickyMs]）解耦：计数窗口到期会清零重算，
 * 但热点标记按独立 TTL 延续，避免窗口边界把热点 build 抖回普通队列。
 *
 * 内存：窗口计数与热点粘性均使用固定容量数组（非无界 ConcurrentHashMap），
 * 容量由 [maxTrackedBuilds] / [maxHeavyBuilds] 限制；冲突时丢弃冷流量统计，偏向保留热点。
 */
@Component
class LogTrafficStatsService {

    /**
     * 是否启用本地窗口吞吐统计。
     * false：不 record、不判定热点，所有 origin 走普通队列（即使 [routeHeavyEnabled]=true）。
     */
    @Value("\${log.traffic.enabled:true}")
    private var enabled: Boolean = true

    /**
     * 定时日志打印的 topN 热点 build 数量（仅观测，不影响分流决策）。
     */
    @Value("\${log.traffic.topN:10}")
    private var topN: Int = 10

    /**
     * 本地行数累计窗口长度（毫秒）。窗口到期会清零重算；热点粘性由 [heavyStickyMs] 独立维持。
     */
    @Value("\${log.traffic.windowMs:30000}")
    private var windowMs: Long = 30_000L

    /**
     * 单个 buildId 在窗口内累计上报行数达到该阈值后视为热点，写入粘性表。
     * 调低：更容易被标为热点并分流；调高：仅极端突发构建进入 heavy。
     */
    @Value("\${log.traffic.heavyThreshold:20000}")
    private var heavyThreshold: Long = 20_000L

    /**
     * 热点标记延续时间（毫秒）：判定为热点后，在该时长内持续可走 heavy 队列，跨计数窗口生效。
     * 避免窗口边界把热点 build 抖回普通队列。
     */
    @Value("\${log.traffic.heavyStickyMs:60000}")
    private var heavyStickyMs: Long = 60_000L

    /**
     * 是否将热点 build 投放到独立 heavy origin 队列。
     * 默认 false，兼容未创建 heavy topic 的环境；开启前需确保 MQ destination 已就绪。
     * 影响：[com.tencent.devops.log.service.BuildLogPrintService] 的队列投放决策。
     */
    @Value("\${log.traffic.routeHeavyEnabled:false}")
    private var routeHeavyEnabled: Boolean = false

    /**
     * 窗口内最多跟踪的 build 槽位数（固定数组容量）。
     * 生产常有上万并发构建，默认需远高于「仅 top 热点」规模，避免窗口内冷流量占满槽位后漏检真热点。
     * 内存开销很小（约数千槽位量级）；冲突时优先替换探测范围内累计更低的冷槽。
     */
    @Value("\${log.traffic.maxTrackedBuilds:8192}")
    private var maxTrackedBuilds: Int = 8192

    /**
     * 同时处于热点粘性窗口的 build 上限（固定数组容量）。
     * 仅容纳「已达 [heavyThreshold]」的热点，不是全量并发构建数；
     * 上万并发下同时成为热点的通常远少于此，但默认需留足突发余量。满时替换最早到期条目。
     */
    @Value("\${log.traffic.maxHeavyBuilds:512}")
    private var maxHeavyBuilds: Int = 512

    @Volatile
    private lateinit var window: Window

    // 热点粘性表：固定容量，独立于计数窗口
    private lateinit var heavyTable: HeavyStickyTable

    @PostConstruct
    fun init() {
        window = Window(System.currentTimeMillis(), BuildLineSlots(trackedCapacity()))
        heavyTable = HeavyStickyTable(heavyCapacity())
    }

    fun record(buildId: String, lines: Int) {
        if (!enabled || buildId.isBlank() || lines <= 0) {
            return
        }
        val sum = currentWindow().slots.add(buildId, lines.toLong(), heavyThreshold)
        if (sum >= heavyThreshold) {
            heavyTable.mark(buildId, System.currentTimeMillis() + heavyStickyMs)
        }
    }

    /**
     * 是否应将后续 origin 日志投放到 heavy 队列。
     */
    fun shouldRouteHeavy(buildId: String): Boolean {
        if (!enabled || !routeHeavyEnabled || buildId.isBlank()) {
            return false
        }
        return heavyTable.isActive(buildId, System.currentTimeMillis())
    }

    /** 当前仍处于热点粘性窗口内的 build 数量，供监控 Gauge 使用 */
    fun heavySize(): Int = heavyTable.size(System.currentTimeMillis())

    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    fun printTopTraffic() {
        if (!enabled) {
            return
        }
        val now = System.currentTimeMillis()
        val buildTop = window.slots.top(topN)
        val heavySize = heavyTable.size(now)
        if (buildTop.isEmpty() && heavySize == 0) {
            return
        }
        logger.info(
            "Log traffic top{} builds in local window({}ms), heavyThreshold={}, " +
                "routeHeavyEnabled={}, heavySize={}, maxTracked={}, maxHeavy={}, builds={}",
            topN,
            windowMs,
            heavyThreshold,
            routeHeavyEnabled,
            heavySize,
            trackedCapacity(),
            heavyCapacity(),
            buildTop
        )
    }

    private fun currentWindow(): Window {
        val now = System.currentTimeMillis()
        var current = window
        if (now - current.startMs < windowMs) {
            return current
        }
        synchronized(this) {
            current = window
            if (now - current.startMs >= windowMs) {
                window = Window(now, BuildLineSlots(trackedCapacity()))
                current = window
            }
        }
        return current
    }

    private fun trackedCapacity(): Int = maxTrackedBuilds.coerceAtLeast(topN).coerceAtLeast(16)

    private fun heavyCapacity(): Int = maxHeavyBuilds.coerceAtLeast(topN).coerceAtLeast(8)

    /**
     * 固定容量的 build 行数计数槽：hash 定位 + 有限探测；冲突时不扩容。
     * 探测范围内无空槽时，替换累计行数低于 [protectThreshold] 的最冷槽，避免冷流量占满后漏掉热点。
     */
    private class BuildLineSlots(val capacity: Int) {
        private val slots = Array(capacity) { Slot() }

        fun add(buildId: String, delta: Long, protectThreshold: Long): Long {
            val start = index(buildId)
            // 先找已占用本 buildId 的槽
            for (probe in 0 until PROBE_LIMIT) {
                val slot = slots[(start + probe) % capacity]
                synchronized(slot) {
                    if (slot.buildId == buildId) {
                        slot.lines.add(delta)
                        return slot.lines.sum()
                    }
                }
            }
            // 再尝试占用空槽，或记录可替换的最冷槽
            var victim: Slot? = null
            var victimSum = Long.MAX_VALUE
            for (probe in 0 until PROBE_LIMIT) {
                val slot = slots[(start + probe) % capacity]
                synchronized(slot) {
                    when (slot.buildId) {
                        null -> {
                            slot.buildId = buildId
                            slot.lines.reset()
                            slot.lines.add(delta)
                            return slot.lines.sum()
                        }
                        buildId -> {
                            slot.lines.add(delta)
                            return slot.lines.sum()
                        }
                        else -> {
                            val sum = slot.lines.sum()
                            // 已接近/超过热点阈值的槽受保护，不被冷流量挤掉
                            if (sum < protectThreshold && sum < victimSum) {
                                victim = slot
                                victimSum = sum
                            }
                        }
                    }
                }
            }
            val replace = victim ?: return 0L
            synchronized(replace) {
                // 再次确认：仍是可替换冷槽，或已被同 buildId 占用
                when (replace.buildId) {
                    buildId -> {
                        replace.lines.add(delta)
                        return replace.lines.sum()
                    }
                    null -> {
                        replace.buildId = buildId
                        replace.lines.reset()
                        replace.lines.add(delta)
                        return replace.lines.sum()
                    }
                    else -> {
                        if (replace.lines.sum() >= protectThreshold) {
                            return 0L
                        }
                        replace.buildId = buildId
                        replace.lines.reset()
                        replace.lines.add(delta)
                        return replace.lines.sum()
                    }
                }
            }
        }

        fun top(n: Int): List<Pair<String, Long>> {
            val result = ArrayList<Pair<String, Long>>(minOf(n, capacity))
            for (slot in slots) {
                val id = slot.buildId ?: continue
                val sum = slot.lines.sum()
                if (sum > 0) {
                    result.add(id to sum)
                }
            }
            return result.sortedByDescending { it.second }.take(n)
        }

        private fun index(buildId: String): Int =
            (buildId.hashCode() and Int.MAX_VALUE) % capacity

        private class Slot {
            @Volatile
            var buildId: String? = null
            val lines = LongAdder()
        }

        companion object {
            private const val PROBE_LIMIT = 8
        }
    }

    /**
     * 固定容量的热点粘性表：线性扫描（容量小，如默认 64），满时替换最早到期的条目。
     */
    private class HeavyStickyTable(val capacity: Int) {
        private val buildIds = arrayOfNulls<String>(capacity)
        private val untilMs = LongArray(capacity)
        private val lock = Any()

        fun mark(buildId: String, until: Long) {
            synchronized(lock) {
                var emptyIdx = -1
                var victimIdx = 0
                var victimUntil = Long.MAX_VALUE
                for (i in 0 until capacity) {
                    val id = buildIds[i]
                    when {
                        id == buildId -> {
                            untilMs[i] = until
                            return
                        }
                        id == null -> if (emptyIdx < 0) emptyIdx = i
                        else -> if (untilMs[i] < victimUntil) {
                            victimUntil = untilMs[i]
                            victimIdx = i
                        }
                    }
                }
                val idx = if (emptyIdx >= 0) emptyIdx else victimIdx
                buildIds[idx] = buildId
                untilMs[idx] = until
            }
        }

        fun isActive(buildId: String, now: Long): Boolean {
            synchronized(lock) {
                for (i in 0 until capacity) {
                    if (buildIds[i] != buildId) {
                        continue
                    }
                    if (untilMs[i] > now) {
                        return true
                    }
                    buildIds[i] = null
                    untilMs[i] = 0L
                    return false
                }
                return false
            }
        }

        fun size(now: Long): Int {
            synchronized(lock) {
                var count = 0
                for (i in 0 until capacity) {
                    if (buildIds[i] == null) {
                        continue
                    }
                    if (untilMs[i] > now) {
                        count++
                    } else {
                        buildIds[i] = null
                        untilMs[i] = 0L
                    }
                }
                return count
            }
        }
    }

    private data class Window(
        val startMs: Long,
        val slots: BuildLineSlots
    )

    companion object {
        private val logger = LoggerFactory.getLogger(LogTrafficStatsService::class.java)
    }
}

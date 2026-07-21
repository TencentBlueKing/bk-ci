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

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
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
 */
@Component
class LogTrafficStatsService {

    @Value("\${log.traffic.enabled:true}")
    private var enabled: Boolean = true

    @Value("\${log.traffic.topN:10}")
    private var topN: Int = 10

    @Value("\${log.traffic.windowMs:30000}")
    private var windowMs: Long = 30_000L

    /**
     * 单个 buildId 在窗口内累计上报行数达到该阈值后视为热点。
     */
    @Value("\${log.traffic.heavyThreshold:20000}")
    private var heavyThreshold: Long = 20_000L

    /**
     * 热点标记延续时间：被判定为热点后，在该时长内持续走 heavy 队列，跨计数窗口生效。
     */
    @Value("\${log.traffic.heavyStickyMs:60000}")
    private var heavyStickyMs: Long = 60_000L

    /**
     * 是否将热点 build 投放到独立 heavy origin 队列。默认关闭，兼容未创建 topic 的环境。
     */
    @Value("\${log.traffic.routeHeavyEnabled:false}")
    private var routeHeavyEnabled: Boolean = false

    @Volatile
    private var window = newWindow()

    // buildId -> 热点标记到期时间戳；独立于计数窗口，保证热点分流的粘性
    private val heavyUntil = ConcurrentHashMap<String, Long>()

    fun record(buildId: String, lines: Int) {
        if (!enabled || buildId.isBlank() || lines <= 0) {
            return
        }
        val adder = currentWindow().buildLines.computeIfAbsent(buildId) { LongAdder() }
        adder.add(lines.toLong())
        if (adder.sum() >= heavyThreshold) {
            heavyUntil[buildId] = System.currentTimeMillis() + heavyStickyMs
        }
    }

    /**
     * 是否应将后续 origin 日志投放到 heavy 队列。
     */
    fun shouldRouteHeavy(buildId: String): Boolean {
        if (!enabled || !routeHeavyEnabled || buildId.isBlank()) {
            return false
        }
        val until = heavyUntil[buildId] ?: return false
        if (System.currentTimeMillis() >= until) {
            heavyUntil.remove(buildId, until)
            return false
        }
        return true
    }

    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    fun printTopTraffic() {
        if (!enabled) {
            return
        }
        purgeExpiredHeavy()
        val snapshot = window
        val buildTop = snapshot.buildLines.entries
            .map { it.key to it.value.sum() }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(topN)
        if (buildTop.isEmpty() && heavyUntil.isEmpty()) {
            return
        }
        logger.info(
            "Log traffic top{} builds in local window({}ms), heavyThreshold={}, " +
                "routeHeavyEnabled={}, heavySize={}, builds={}",
            topN,
            windowMs,
            heavyThreshold,
            routeHeavyEnabled,
            heavyUntil.size,
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
                window = newWindow(now)
                current = window
            }
        }
        return current
    }

    private fun purgeExpiredHeavy() {
        val now = System.currentTimeMillis()
        heavyUntil.entries.removeIf { it.value <= now }
    }

    private fun newWindow(startMs: Long = System.currentTimeMillis()) = Window(
        startMs = startMs,
        buildLines = ConcurrentHashMap()
    )

    private data class Window(
        val startMs: Long,
        val buildLines: ConcurrentHashMap<String, LongAdder>
    )

    companion object {
        private val logger = LoggerFactory.getLogger(LogTrafficStatsService::class.java)
    }
}

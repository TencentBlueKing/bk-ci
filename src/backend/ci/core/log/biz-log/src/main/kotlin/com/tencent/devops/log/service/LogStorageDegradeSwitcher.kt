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

import com.tencent.devops.log.configuration.LogDegradeProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * origin 直写 ES 的 per-project（per-key）熔断器。
 *
 * trafficKey 通常是 projectId，回退场景下是 `b:{buildId}`。
 * 每个 key 独立维护滑动窗口 + 熔断状态，单个项目故障不影响其他项目。
 * [forceStorage][LogDegradeProperties.forceStorage] 仍是全局开关。
 */
@Component
@ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "elasticsearch")
class LogStorageDegradeSwitcher(
    private val properties: LogDegradeProperties
) {

    private data class Sample(
        val timestamp: Long,
        val success: Boolean,
        val latencyMs: Long
    )

    private class KeyState {
        val samples = ConcurrentLinkedQueue<Sample>()
        val circuitOpenUntil = AtomicLong(0)
        @Volatile var lastAccessMs: Long = System.currentTimeMillis()
    }

    private val keyStates = ConcurrentHashMap<String, KeyState>()
    private val degradeCount = AtomicLong(0)
    private val lastEvictMs = AtomicLong(0)

    fun shouldDegrade(trafficKey: String? = null): Boolean {
        if (properties.forceStorage) {
            return true
        }
        if (!properties.enabled) {
            return false
        }
        val key = normalizeKey(trafficKey)
        val state = keyStates[key] ?: return false
        state.lastAccessMs = System.currentTimeMillis()
        return System.currentTimeMillis() < state.circuitOpenUntil.get()
    }

    fun recordSuccess(latencyMs: Long, trafficKey: String? = null) {
        val key = normalizeKey(trafficKey)
        val state = getOrCreateState(key)
        addSample(state, success = true, latencyMs = latencyMs)
    }

    fun recordFailure(latencyMs: Long, trafficKey: String? = null) {
        val key = normalizeKey(trafficKey)
        val state = getOrCreateState(key)
        addSample(state, success = false, latencyMs = latencyMs)
        maybeOpenCircuit(key, state)
    }

    fun recordDegrade() {
        degradeCount.incrementAndGet()
    }

    fun getDegradeCount(): Long = degradeCount.get()

    fun isCircuitOpen(): Boolean {
        val now = System.currentTimeMillis()
        return keyStates.values.any { now < it.circuitOpenUntil.get() }
    }

    fun openCircuitProjectCount(): Int {
        val now = System.currentTimeMillis()
        return keyStates.values.count { now < it.circuitOpenUntil.get() }
    }

    private fun getOrCreateState(key: String): KeyState {
        val state = keyStates.computeIfAbsent(key) { KeyState() }
        state.lastAccessMs = System.currentTimeMillis()
        evictIfNeeded()
        return state
    }

    private fun addSample(state: KeyState, success: Boolean, latencyMs: Long) {
        val now = System.currentTimeMillis()
        state.samples.add(Sample(now, success, latencyMs))
        trim(state, now)
    }

    private fun maybeOpenCircuit(key: String, state: KeyState) {
        val now = System.currentTimeMillis()
        trim(state, now)
        val snapshot = state.samples.toList()
        if (snapshot.size < properties.circuitMinSamples) {
            return
        }
        val failures = snapshot.count { !it.success || it.latencyMs > properties.slowMs }
        val failRate = failures.toDouble() / snapshot.size
        if (failRate >= properties.circuitFailRate) {
            val openUntil = now + properties.circuitOpenMs
            state.circuitOpenUntil.set(openUntil)
            logger.warn(
                "Log ES direct-write circuit open for key={} until {}, failRate={}, samples={}",
                key, openUntil, failRate, snapshot.size
            )
        }
    }

    private fun trim(state: KeyState, now: Long) {
        val expireBefore = now - properties.circuitWindowMs
        while (true) {
            val head = state.samples.peek() ?: return
            if (head.timestamp >= expireBefore) {
                return
            }
            state.samples.poll()
        }
    }

    /**
     * 淘汰最冷的 key。
     *
     * 这里位于每批日志都会走到的记录路径上，而 key 数量在回退到 `b:{buildId}` 时等于
     * 并发构建数，很容易长期高于上限。因此不能每次都做全量排序：先留出一段冗余水位，
     * 超过后才批量清理到上限以下，并按时间节流，避免持续排序拖慢消费。
     */
    private fun evictIfNeeded() {
        val maxSize = properties.maxTrackedProjects.coerceAtLeast(1)
        if (keyStates.size <= maxSize + maxSize / EVICT_SLACK_DIVISOR) return
        val now = System.currentTimeMillis()
        val last = lastEvictMs.get()
        if (now - last < EVICT_INTERVAL_MS || !lastEvictMs.compareAndSet(last, now)) return
        val retain = maxSize - maxSize / EVICT_SLACK_DIVISOR
        val toRemove = keyStates.size - retain
        if (toRemove <= 0) return
        keyStates.entries
            .sortedBy { it.value.lastAccessMs }
            .take(toRemove)
            .forEach { keyStates.remove(it.key) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LogStorageDegradeSwitcher::class.java)
        private const val UNKNOWN_KEY = "#unknown"

        /** 上限之上额外容忍 1/8 的冗余，避免在上限附近反复触发淘汰 */
        private const val EVICT_SLACK_DIVISOR = 8

        private const val EVICT_INTERVAL_MS = 5_000L

        private fun normalizeKey(trafficKey: String?): String {
            return trafficKey?.takeIf { it.isNotBlank() } ?: UNKNOWN_KEY
        }
    }
}

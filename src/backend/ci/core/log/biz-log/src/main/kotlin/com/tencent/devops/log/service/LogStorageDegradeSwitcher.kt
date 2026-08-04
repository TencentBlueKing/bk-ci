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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * origin 直写 ES 的熔断器：失败率或慢请求过高时，短期切到 storage 队列容灾。
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

    private val samples = ConcurrentLinkedQueue<Sample>()
    private val circuitOpenUntil = AtomicLong(0)
    private val degradeCount = AtomicLong(0)

    fun shouldDegrade(): Boolean {
        if (properties.forceStorage) {
            return true
        }
        if (!properties.enabled) {
            return false
        }
        return System.currentTimeMillis() < circuitOpenUntil.get()
    }

    fun recordSuccess(latencyMs: Long) {
        addSample(success = true, latencyMs = latencyMs)
    }

    fun recordFailure(latencyMs: Long) {
        addSample(success = false, latencyMs = latencyMs)
        maybeOpenCircuit()
    }

    fun recordDegrade() {
        degradeCount.incrementAndGet()
    }

    fun getDegradeCount(): Long = degradeCount.get()

    fun isCircuitOpen(): Boolean = System.currentTimeMillis() < circuitOpenUntil.get()

    private fun addSample(success: Boolean, latencyMs: Long) {
        val now = System.currentTimeMillis()
        samples.add(Sample(now, success, latencyMs))
        trim(now)
    }

    private fun maybeOpenCircuit() {
        val now = System.currentTimeMillis()
        trim(now)
        val snapshot = samples.toList()
        if (snapshot.size < properties.circuitMinSamples) {
            return
        }
        val failures = snapshot.count { !it.success || it.latencyMs > properties.slowMs }
        val failRate = failures.toDouble() / snapshot.size
        if (failRate >= properties.circuitFailRate) {
            val openUntil = now + properties.circuitOpenMs
            circuitOpenUntil.set(openUntil)
            logger.warn(
                "Log ES direct-write circuit open until {}, failRate={}, samples={}",
                openUntil,
                failRate,
                snapshot.size
            )
        }
    }

    private fun trim(now: Long) {
        val expireBefore = now - properties.circuitWindowMs
        while (true) {
            val head = samples.peek() ?: return
            if (head.timestamp >= expireBefore) {
                return
            }
            samples.poll()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LogStorageDegradeSwitcher::class.java)
    }
}

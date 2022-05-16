/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.metrics

import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus
import com.tencent.bkrepo.scanner.pojo.ScanTaskStatus
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 扫描服务数据统计
 */
@Component
class ScannerMetrics(
    private val meterRegistry: MeterRegistry,
    // 一个任务可能被不同的服务实例处理，统计数据需要放到公共存储上才能保证数据准确
    private val redisOperation: RedisOperation
) {
    /**
     * 记录本服务实例上各状态任务数量
     */
    private val localTaskCountMap = ConcurrentHashMap<String, AtomicLong>(ScanTaskStatus.values().size)
    private val taskCountMap = ConcurrentHashMap<String, RedisAtomicLong>(ScanTaskStatus.values().size)
    private val subtaskCounterMap = ConcurrentHashMap<String, RedisAtomicLong>(SubScanTaskStatus.values().size)
    private val subtaskSpeedSummaryMap = ConcurrentHashMap<String, DistributionSummary>()
    private val reuseResultSubtaskCounters: RedisAtomicLong by lazy {
        // 统计重用扫描结果的子任务数量
        val atomicLong = RedisAtomicLong(redisOperation, SCANNER_SUBTASK_REUSE_RESULT_COUNT)
        Gauge.builder(SCANNER_SUBTASK_REUSE_RESULT_COUNT, atomicLong, RedisAtomicLong::toDouble)
            .register(meterRegistry)
        atomicLong
    }

    /**
     * 处于[status]状态的任务数量增加[count]个
     */
    fun incTaskCountAndGet(status: ScanTaskStatus, count: Long = 1): Long {
        if (status == ScanTaskStatus.SCANNING_SUBMITTING) {
            taskCounter(ScanTaskStatus.PENDING).addAndGet(-count)
            localTaskCounter(ScanTaskStatus.PENDING).addAndGet(-count)
        }
        if (status == ScanTaskStatus.SCANNING_SUBMITTED) {
            taskCounter(ScanTaskStatus.SCANNING_SUBMITTING).addAndGet(-count)
            localTaskCounter(ScanTaskStatus.SCANNING_SUBMITTING).addAndGet(-count)
        }
        if (status == ScanTaskStatus.FINISHED) {
            taskCounter(ScanTaskStatus.SCANNING_SUBMITTED).addAndGet(-count)
        }

        // 单个服务实例仅统计PENDING和SCANNING_SUBMITTING状态的任务数量
        if (status == ScanTaskStatus.PENDING || status == ScanTaskStatus.SCANNING_SUBMITTING) {
            localTaskCounter(status).addAndGet(count)
        }
        return taskCounter(status).addAndGet(count)
    }

    /**
     * [pre]状态任务数量减1，[next]状态任务数量加1
     */
    fun taskStatusChange(pre: ScanTaskStatus, next: ScanTaskStatus) {
        if (next == ScanTaskStatus.PENDING) {
            localTaskCounter(next).incrementAndGet()
        }
        taskCounter(pre).decrementAndGet()
        taskCounter(next).incrementAndGet()
    }

    /**
     * 处于[status]状态的任务数量增加[count]个
     */
    fun incSubtaskCountAndGet(status: SubScanTaskStatus, count: Long = 1): Long {
        return subtaskCounter(status).addAndGet(count)
    }

    /**
     * 处于[status]状态的任务数量减少[count]个
     */
    fun decSubtaskCountAndGet(status: SubScanTaskStatus, count: Long = 1): Long {
        return subtaskCounter(status).addAndGet(-count)
    }

    /**
     * [pre]状态任务数量减1，[next]状态任务数量加1
     */
    fun subtaskStatusChange(pre: SubScanTaskStatus, next: SubScanTaskStatus) {
        subtaskCounter(pre).decrementAndGet()
        subtaskCounter(next).incrementAndGet()
    }

    /**
     * 重用扫描结果的子任务数量加1
     */
    fun incReuseResultSubtaskCount(count: Long = 1) {
        reuseResultSubtaskCounters.addAndGet(count)
    }

    fun record(
        fullPath: String,
        fileSize: Long,
        scanner: String,
        startTimestamp: Long,
        finishedTimestamp: Long
    ) {
        val fileExtensionName = fullPath.substringAfterLast('.', UNKNOWN_EXTENSION)
        val summary = taskSpeedSummary(fileExtensionName, scanner)
        val elapsedSeconds = (finishedTimestamp - startTimestamp).toDouble() / 1000
        summary.record(fileSize / elapsedSeconds)
    }

    private fun subtaskCounter(status: SubScanTaskStatus): RedisAtomicLong {
        return subtaskCounterMap.getOrPut(status.name) {
            // 统计不同状态扫描任务数量
            val key = metricsKey(SCANNER_SUBTASK_COUNT, "status", status.name)
            val atomicLong = RedisAtomicLong(redisOperation, key)
            Gauge.builder(SCANNER_SUBTASK_COUNT, atomicLong, RedisAtomicLong::toDouble)
                .description("${status.name} subtask count")
                .tag("status", status.name)
                .register(meterRegistry)
            atomicLong
        }
    }

    private fun localTaskCounter(status: ScanTaskStatus): AtomicLong {
        return localTaskCountMap.getOrPut(status.name) {
            // 统计不同状态子任务数量
            val atomicLong = AtomicLong(0L)
            val gauge = taskGauge(atomicLong, AtomicLong::toDouble, status, true)
            gauge.register(meterRegistry)
            atomicLong
        }
    }

    private fun taskCounter(status: ScanTaskStatus): RedisAtomicLong {
        return taskCountMap.getOrPut(status.name) {
            // 统计不同状态子任务数量
            val key = metricsKey(SCANNER_TASK_COUNT, "status", status.name)
            val atomicLong = RedisAtomicLong(redisOperation, key)
            taskGauge(atomicLong, RedisAtomicLong::toDouble, status).register(meterRegistry)
            atomicLong
        }
    }

    private fun <T> taskGauge(
        obj: T,
        f: T.() -> Double,
        status: ScanTaskStatus,
        local: Boolean = false
    ): Gauge.Builder<T> {
        return Gauge.builder(SCANNER_TASK_COUNT, obj, f)
            .description("${status.name} task count")
            .tag("status", status.name)
            .tag("local", local.toString())
    }

    private fun taskSpeedSummary(fileType: String, scanner: String): DistributionSummary {
        return subtaskSpeedSummaryMap.getOrPut(speedSummaryCacheKey(fileType, scanner)) {
            DistributionSummary.builder(SCANNER_SUBTASK_SPEED)
                .description("subtask speed")
                .baseUnit("bytes/seconds")
                .tag("fileType", fileType)
                .tag("scanner", scanner)
                .register(meterRegistry)
        }
    }

    private fun speedSummaryCacheKey(fileType: String, scanner: String) =
        "$fileType:$scanner"

    private fun metricsKey(meterName: String, vararg tags: String): String {
        val newMeterName = meterName.removePrefix("scanner.").replace(".", ":")
        return "metrics:scanner:$newMeterName:${tags.joinToString(":")}"
    }

    companion object {
        /**
         * 扫描任务数量
         */
        private const val SCANNER_TASK_COUNT = "scanner.task.count"

        /**
         * 子扫描任务数量
         */
        private const val SCANNER_SUBTASK_COUNT = "scanner.subtask.count"

        /**
         * 重用扫描结果的子任务数量
         */
        private const val SCANNER_SUBTASK_REUSE_RESULT_COUNT = "scanner.subtask.reuse-result.count"

        /**
         * 子任务执行耗时
         */
        private const val SCANNER_SUBTASK_SPEED = "scanner.subtask.speed"

        /**
         * 未知扩展名
         */
        private const val UNKNOWN_EXTENSION = "UNKNOWN"
    }
}

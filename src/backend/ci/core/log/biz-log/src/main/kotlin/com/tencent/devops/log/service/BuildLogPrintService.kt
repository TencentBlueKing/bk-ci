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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.log.configuration.LogServiceConfig
import com.tencent.devops.log.configuration.StorageProperties
import com.tencent.devops.log.event.ILogEvent
import com.tencent.devops.log.event.LogOriginEvent
import com.tencent.devops.log.event.LogOriginHeavyEvent
import com.tencent.devops.log.event.LogStatusEvent
import com.tencent.devops.log.event.LogStorageEvent
import com.tencent.devops.log.jmx.LogPrintBean
import com.tencent.devops.log.meta.Ansi
import com.tencent.devops.log.util.LogErrorCodeEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Suppress("MagicNumber")
class BuildLogPrintService @Autowired constructor(
    private val streamBridge: StreamBridge,
    private val logPrintBean: LogPrintBean,
    private val storageProperties: StorageProperties,
    private val logProjectIdResolver: LogProjectIdResolver,
    private val logTrafficStatsService: LogTrafficStatsService,
    logServiceConfig: LogServiceConfig
) {

    private val logExecutorService = ThreadPoolExecutor(
        logServiceConfig.corePoolSize ?: 100,
        logServiceConfig.maxPoolSize ?: 100,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(logServiceConfig.taskQueueSize ?: 1000)
    )

    /**
     * @param recordTraffic 仅上报入口统计流量并决定队列投放；MQ 重试/降级转发传 false
     */
    fun dispatchEvent(event: ILogEvent, recordTraffic: Boolean = false) {
        enrichAndSend(event, recordTraffic)
    }

    fun asyncDispatchEvent(event: ILogEvent, recordTraffic: Boolean = true): Result<Boolean> {
        val enriched = enrichProjectId(event)
        if (!isEnabled(storageProperties.enable)) {
            val warnings = "Service refuses to write the log, the log file of the task will be archived."
            if (enriched is LogOriginEvent && enriched.logs.isNotEmpty()) {
                dispatchEvent(
                    event = enriched.copy(
                        logs = listOf(
                            enriched.logs.first().copy(
                                message = Ansi().fgYellow().a(warnings).reset().toString(),
                                logType = LogType.WARN
                            )
                        )
                    ),
                    recordTraffic = false
                )
            }
            return Result(
                status = 503,
                message = LogErrorCodeEnum.PRINT_IS_DISABLED.formatErrorMessage,
                data = false
            )
        }
        return try {
            logExecutorService.execute {
                enrichAndSend(enriched, recordTraffic)
            }
            Result(true)
        } catch (e: RejectedExecutionException) {
            // 队列满时的处理逻辑
            logger.warn(
                "BuildLogPrintService[${enriched.buildId}] " +
                    "asyncDispatchEvent failed with queue tasks exceed the limit",
                e
            )
            Result(
                status = 509,
                message = LogErrorCodeEnum.PRINT_QUEUE_LIMIT.formatErrorMessage,
                data = false
            )
        }
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    fun logExecutorPerformance() {
        logPrintBean.savePrintTaskCount(logExecutorService.taskCount)
        logPrintBean.savePrintActiveCount(logExecutorService.activeCount)
        logPrintBean.savePrintQueueSize(logExecutorService.queue.size)
    }

    private fun enrichAndSend(event: ILogEvent, recordTraffic: Boolean) {
        val enriched = enrichProjectId(event)
        when {
            // 已在 heavy 队列内的重试/转发，保持原 destination
            enriched is LogOriginHeavyEvent -> enriched.sendTo(streamBridge)
            enriched is LogOriginEvent && recordTraffic -> {
                logTrafficStatsService.record(enriched.buildId, enriched.logs.size)
                if (logTrafficStatsService.shouldRouteHeavy(enriched.buildId)) {
                    LogOriginHeavyEvent.from(enriched).sendTo(streamBridge)
                } else {
                    enriched.sendTo(streamBridge)
                }
            }
            else -> {
                if (recordTraffic) {
                    recordTrafficLines(enriched)
                }
                enriched.sendTo(streamBridge)
            }
        }
    }

    private fun enrichProjectId(event: ILogEvent): ILogEvent {
        val resolved = logProjectIdResolver.resolve(event.buildId, event.projectId)
        if (resolved.isNullOrBlank() || resolved == event.projectId) {
            return event
        }
        return when (event) {
            is LogOriginEvent -> event.copy(projectId = resolved)
            is LogOriginHeavyEvent -> event.copy(projectId = resolved)
            is LogStorageEvent -> event.copy(projectId = resolved)
            is LogStatusEvent -> event.copy(projectId = resolved)
            else -> event
        }
    }

    private fun recordTrafficLines(event: ILogEvent) {
        val lines = when (event) {
            is LogOriginEvent -> event.logs.size
            is LogOriginHeavyEvent -> event.logs.size
            is LogStorageEvent -> event.logs.size
            else -> 0
        }
        if (lines > 0) {
            logTrafficStatsService.record(event.buildId, lines)
        }
    }

    private fun isEnabled(value: String?): Boolean {
        // 假设没有配置默认为开启日志保存
        return if (!value.isNullOrBlank()) {
            value.toBoolean()
        } else {
            true
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildLogPrintService::class.java)
    }
}

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
    logServiceConfig: LogServiceConfig
) {

    private val logExecutorService = ThreadPoolExecutor(
        logServiceConfig.corePoolSize ?: 100,
        logServiceConfig.maxPoolSize ?: 100,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(logServiceConfig.taskQueueSize ?: 1000)
    )

    fun dispatchEvent(event: ILogEvent) {
        event.sendTo(streamBridge)
    }

    fun asyncDispatchEvent(event: ILogEvent): Result<Boolean> {
        if (!isEnabled(storageProperties.enable)) {
            val warnings = "Service refuses to write the log, the log file of the task will be archived."
            if (event is LogOriginEvent && event.logs.isNotEmpty()) {
                dispatchEvent(
                    event.copy(
                        logs = listOf(
                            event.logs.first().copy(
                                message = Ansi().fgYellow().a(warnings).reset().toString(),
                                logType = LogType.WARN
                            )
                        )
                    )
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
                dispatchEvent(event)
            }
            Result(true)
        } catch (e: RejectedExecutionException) {
            // 队列满时的处理逻辑
            logger.error(
                "BKSystemErrorMonitor|BuildLogPrintService[${event.buildId}] " +
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

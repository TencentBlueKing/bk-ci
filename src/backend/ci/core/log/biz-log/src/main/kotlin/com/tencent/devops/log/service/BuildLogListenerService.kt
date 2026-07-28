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

import com.tencent.devops.log.event.LogOriginEvent
import com.tencent.devops.log.event.LogOriginHeavyEvent
import com.tencent.devops.log.event.LogStatusEvent
import com.tencent.devops.log.event.LogStorageEvent
import com.tencent.devops.log.metrics.LogMetrics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BuildLogListenerService @Autowired constructor(
    private val logService: LogService,
    private val indexService: IndexService,
    private val buildLogPrintService: BuildLogPrintService,
    private val logMetrics: LogMetrics
) {

    fun handleEvent(event: LogOriginEvent) {
        val start = System.currentTimeMillis()
        var result = false
        try {
            logService.addLogEvent(event)
            result = true
        } catch (ignored: Throwable) {
            logger.warn("Fail to add the log event [${event.buildId}|${event.retryTime}]", ignored)
        } finally {
            val retried = !result && event.retryTime >= 0
            logMetrics.recordKafkaConsume(
                destination = LogMetrics.DESTINATION_ORIGIN,
                elapseMs = System.currentTimeMillis() - start,
                success = result,
                retried = retried
            )
            if (retried) {
                logger.warn("Retry to add the log event [${event.buildId}|${event.retryTime}]")
                with(event) {
                    buildLogPrintService.dispatchEvent(
                        event = LogOriginEvent(
                            buildId = buildId,
                            logs = logs,
                            retryTime = retryTime - 1,
                            delayMills = getNextDelayMills(retryTime)
                        ),
                        recordTraffic = false
                    )
                }
            }
        }
    }

    fun handleEvent(event: LogOriginHeavyEvent) {
        val start = System.currentTimeMillis()
        var result = false
        try {
            logService.addLogEvent(event.toOriginEvent())
            result = true
        } catch (ignored: Throwable) {
            logger.warn("Fail to add the heavy log event [${event.buildId}|${event.retryTime}]", ignored)
        } finally {
            val retried = !result && event.retryTime >= 0
            logMetrics.recordKafkaConsume(
                destination = LogMetrics.DESTINATION_ORIGIN_HEAVY,
                elapseMs = System.currentTimeMillis() - start,
                success = result,
                retried = retried
            )
            if (retried) {
                logger.warn("Retry to add the heavy log event [${event.buildId}|${event.retryTime}]")
                with(event) {
                    buildLogPrintService.dispatchEvent(
                        event = LogOriginHeavyEvent(
                            buildId = buildId,
                            logs = logs,
                            retryTime = retryTime - 1,
                            delayMills = getNextDelayMills(retryTime)
                        ),
                        recordTraffic = false
                    )
                }
            }
        }
    }

    fun handleEvent(event: LogStorageEvent) {
        val start = System.currentTimeMillis()
        var result = false
        try {
            logService.addBatchLogEvent(event)
            result = true
        } catch (ignored: Throwable) {
            logger.warn("Fail to add the log batch event [${event.buildId}|${event.retryTime}]", ignored)
        } finally {
            val retried = !result && event.retryTime >= 0
            logMetrics.recordKafkaConsume(
                destination = LogMetrics.DESTINATION_STORAGE,
                elapseMs = System.currentTimeMillis() - start,
                success = result,
                retried = retried
            )
            if (retried) {
                logger.warn("Retry to add log batch event [${event.buildId}|${event.retryTime}]")
                with(event) {
                    buildLogPrintService.dispatchEvent(
                        event = LogStorageEvent(
                            buildId = buildId,
                            logs = logs,
                            retryTime = retryTime - 1,
                            delayMills = getNextDelayMills(retryTime)
                        ),
                        recordTraffic = false
                    )
                }
            }
        }
    }

    fun handleEvent(event: LogStatusEvent) {
        val start = System.currentTimeMillis()
        var result = false
        try {
            logService.updateLogStatus(event)
            // #3089 当收到构建级别的状态刷新时，清理缓存并保存行数
            if (event.jobId.isNullOrBlank() && event.tag.isNullOrBlank()) {
                indexService.flushLineNum2DB(event.buildId)
            }
            result = true
        } catch (ignored: Throwable) {
            logger.warn("Fail to add the multi lines [${event.buildId}|${event.retryTime}]", ignored)
        } finally {
            val retried = !result && event.retryTime >= 0
            logMetrics.recordKafkaConsume(
                destination = LogMetrics.DESTINATION_STATUS,
                elapseMs = System.currentTimeMillis() - start,
                success = result,
                retried = retried
            )
            if (retried) {
                logger.warn("Retry to add the multi lines [${event.buildId}|${event.retryTime}]")
                with(event) {
                    buildLogPrintService.dispatchEvent(
                        event = LogStatusEvent(
                            buildId = buildId,
                            finished = finished,
                            tag = tag,
                            subTag = subTag,
                            jobId = jobId,
                            logStorageMode = logStorageMode,
                            executeCount = executeCount,
                            retryTime = retryTime - 1,
                            delayMills = getNextDelayMills(retryTime),
                            userJobId = userJobId,
                            stepId = stepId
                        ),
                        recordTraffic = false
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildLogListenerService::class.java)
    }
}

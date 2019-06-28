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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.log.mq

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.log.model.pojo.LogBatchEvent
import com.tencent.devops.log.model.pojo.LogEvent
import com.tencent.devops.log.model.pojo.LogStatusEvent
import com.tencent.devops.log.service.PipelineLogService
import com.tencent.devops.log.utils.LogDispatcher
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

/**
 * deng
 * 2019-01-23
 */
@Component
class LogListener constructor(
    private val logService: PipelineLogService,
    private val rabbitTemplate: RabbitTemplate
) {

    fun logEvent(event: LogEvent) {
        var result = false
        try {
            logService.addLogEvent(event)
            result = true
        } catch (ignored: Throwable) {
            logger.warn("Fail to add the log event [${event.buildId}|${event.retryTime}]", ignored)
        } finally {
            if (!result && event.retryTime >= 0) {
                logger.warn("Retry to add the log event [${event.buildId}|${event.retryTime}]")

                with(event) {
                    LogDispatcher.dispatch(rabbitTemplate, LogEvent(buildId, logs, retryTime - 1, DelayMills))
                }
            }
        }
    }

    fun logBatchEvent(event: LogBatchEvent) {
        var result = false
        try {
            logService.addBatchLogEvent(event)
            result = true
        } catch (ignored: Throwable) {
            logger.warn("Fail to add the log batch event [${event.buildId}|${event.retryTime}]", ignored)
        } finally {
            if (!result && event.retryTime >= 0) {
                logger.warn("Retry to add log batch event [${event.buildId}|${event.retryTime}]")
                with(event) {
                    LogDispatcher.dispatch(rabbitTemplate, LogBatchEvent(buildId, logs, retryTime - 1, DelayMills))
                }
            }
        }
    }

    @RabbitListener(
        bindings = [QueueBinding(
            key = MQ.ROUTE_LOG_STATUS_BUILD_EVENT,
            value = Queue(value = MQ.QUEUE_LOG_STATUS_BUILD_EVENT, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_LOG_STATUS_BUILD_EVENT,
                durable = "true", delayed = "true", type = ExchangeTypes.DIRECT
            )
        )]
    )
    fun logStatusEvent(event: LogStatusEvent) {
        var result = false
        try {
            logService.upsertLogStatus(event)
            result = true
        } catch (ignored: Throwable) {
            logger.warn("Fail to add the multi lines [${event.buildId}|${event.retryTime}]", ignored)
        } finally {
            if (!result && event.retryTime >= 0) {
                logger.warn("Retry to add the multi lines [${event.buildId}|${event.retryTime}]")
                with(event) {
                    LogDispatcher.dispatch(
                        rabbitTemplate,
                        LogStatusEvent(buildId, finished, tag, executeCount, retryTime - 1, DelayMills)
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LogListener::class.java)
        private const val DelayMills = 3 * 1000
    }
}
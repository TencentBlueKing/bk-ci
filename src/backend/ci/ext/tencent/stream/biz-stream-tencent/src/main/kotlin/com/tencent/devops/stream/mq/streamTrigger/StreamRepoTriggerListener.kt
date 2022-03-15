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

package com.tencent.devops.stream.mq.streamTrigger

import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.stream.constant.MQ
import com.tencent.devops.stream.trigger.GitCITriggerRepoService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamRepoTriggerListener @Autowired constructor(
    private val gitCITriggerRepoService: GitCITriggerRepoService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamRepoTriggerListener::class.java)
    }

    @RabbitListener(
        bindings = [(QueueBinding(
            key = [MQ.ROUTE_STREAM_TRIGGER_REPO_PIPELINE_EVENT],
            value = Queue(value = MQ.QUEUE_STREAM_TRIGGER_REPO_PIPELINE_EVENT, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_STREAM_TRIGGER_REPO_PIPELINE_EVENT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.DIRECT
            )
        ))]
    )
    fun listenStreamRepoTriggerEvent(event: StreamRepoTriggerEvent) {
        try {
            val traceId = MDC.get(TraceTag.BIZID)
            if (traceId.isNullOrEmpty()) {
                if (!event.traceId.isNullOrEmpty()) {
                    MDC.put(TraceTag.BIZID, event.traceId)
                } else {
                    MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
                }
            }
            run(event)
        } catch (ignore: Throwable) {
            logger.error("Fail to start repo trigger (${event.gitRequestEvent.gitProjectName})", ignore)
        } finally {
            MDC.remove(TraceTag.BIZID)
        }
    }

    private fun run(event: StreamRepoTriggerEvent) {
        val startTime = System.currentTimeMillis()
        gitCITriggerRepoService.repoTriggerBuild(
            triggerPipelineList = event.gitRequestEvent.repoTriggerPipelineList,
            gitRequestEvent = event.gitRequestEvent,
            event = event.event
        )
        logger.info(
            "stream pipeline repo trigger (${event.gitRequestEvent.gitProjectName}) " +
                "from trigger to build time：${System.currentTimeMillis() - startTime}"
        )
    }
}

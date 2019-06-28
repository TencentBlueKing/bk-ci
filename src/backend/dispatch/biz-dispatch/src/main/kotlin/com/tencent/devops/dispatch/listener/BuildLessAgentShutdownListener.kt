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

package com.tencent.devops.dispatch.listener

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.dispatch.service.PipelineDispatchService
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BuildLessAgentShutdownListener @Autowired
constructor(private val pipelineDispatchService: PipelineDispatchService) {

    @RabbitListener(
        bindings = [(QueueBinding(
            key = MQ.ROUTE_BUILD_LESS_AGENT_SHUTDOWN_DISPATCH, value = Queue(
                value = MQ.QUEUE_BUILD_LESS_AGENT_SHUTDOWN_DISPATCH, durable = "true"
            ),
            exchange = Exchange(
                value = MQ.EXCHANGE_BUILD_LESS_AGENT_LISTENER_DIRECT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.DIRECT
            )
        ))]
    )
    fun listenAgentStartUpEvent(pipelineBuildLessDockerAgentShutdownEvent: PipelineBuildLessShutdownDispatchEvent) {
        try {
//            pipelineDispatchService.shutdown(pipelineBuildLessDockerAgentShutdownEvent)
        } catch (ignored: Throwable) {
            logger.error("Fail to start the pipe build($pipelineBuildLessDockerAgentShutdownEvent)", ignored)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildLessAgentShutdownListener::class.java)
    }
}

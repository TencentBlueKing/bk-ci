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

package com.tencent.devops.common.event.dispatcher.pipeline.mq

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent
import com.tencent.devops.common.event.pojo.pipeline.IPipelineRoutableEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

/**
 * 基于MQ实现的流水线事件下发器
 *
 * @version 1.0
 */
class MQEventDispatcher constructor(
    private val rabbitTemplate: RabbitTemplate
) : PipelineEventDispatcher {

    override fun dispatch(vararg events: IPipelineEvent) {
        try {
            events.forEach { event ->
                val eventType = event::class.java.annotations.find { s -> s is Event } as Event
                val routeKey = // 根据 routeKey+后缀 实现动态变换路由Key
                    if (event is IPipelineRoutableEvent && !event.routeKeySuffix.isNullOrBlank()) {
                        eventType.routeKey + event.routeKeySuffix
                    } else {
                        eventType.routeKey
                    }
                logger.info("dispatch the event|Route=$routeKey|exchange=${eventType.exchange}|source=(${event.javaClass.name}:${event.source}-${event.actionType}-${event.pipelineId})")
                rabbitTemplate.convertAndSend(eventType.exchange, routeKey, event) { message ->
                    // 事件中的变量指定
                    when {
                        event.delayMills > 0 -> message.messageProperties.setHeader("x-delay", event.delayMills)
                        eventType.delayMills > 0 -> // 事件类型固化默认值
                            message.messageProperties.setHeader("x-delay", eventType.delayMills)
                        else -> // 非延时消息的则8小时后过期，防止意外发送的消息无消费端ACK处理从而堆积过多消息导致MQ故障
                            message.messageProperties.expiration = "28800000"
                    }
                    message
                }
            }
        } catch (e: Exception) {
            logger.error("Fail to dispatch the event($events)", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MQEventDispatcher::class.java)
    }
}

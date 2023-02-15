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

package com.tencent.devops.auth.refresh.dispatch

import com.rabbitmq.client.ChannelContinuationTimeoutException
import com.rabbitmq.client.impl.AMQImpl
import com.tencent.devops.auth.refresh.event.RefreshBroadCastEvent
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.EventDispatcher
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AuthRefreshDispatch @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate
) : EventDispatcher<RefreshBroadCastEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(AuthRefreshDispatch::class.java)
    }

    @SuppressWarnings("NestedBlockDepth")
    override fun dispatch(vararg events: RefreshBroadCastEvent) {
        events.forEach { event ->
            try {
                send(event)
            } catch (ignored: Exception) {
                if (ignored.cause is ChannelContinuationTimeoutException) {
                    logger.warn("[ENGINE_MQ_SEVERE]Fail to dispatch the event($event)", ignored)
                    val cause = ignored.cause as ChannelContinuationTimeoutException
                    if (cause.method is AMQImpl.Channel.Open) {
                        send(event)
                    }
                } else {
                    logger.error("[ENGINE_MQ_SEVERE]Fail to dispatch the event($event)", ignored)
                }
            }
        }
    }

    private fun send(event: RefreshBroadCastEvent) {
        val eventType = event::class.java.annotations.find { s -> s is Event } as Event
        val routeKey = eventType.routeKey
        logger.info("[${eventType.exchange}|$routeKey|${event.refreshType} dispatch the refresh event")
        rabbitTemplate.convertAndSend(eventType.exchange, routeKey, event) { message ->
            if (eventType.delayMills > 0) { // 事件类型固化默认值
                message.messageProperties.setHeader("x-delay", eventType.delayMills)
            }
            message
        }
    }
}

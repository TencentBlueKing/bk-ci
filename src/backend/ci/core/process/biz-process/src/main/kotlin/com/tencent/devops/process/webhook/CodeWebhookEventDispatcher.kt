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

package com.tencent.devops.process.webhook

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.process.webhook.pojo.event.commit.GithubWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.ICodeWebhookEvent
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

/**
 * @ Date       ：Created in 10:32 2019-08-08
 */

object CodeWebhookEventDispatcher {

    fun dispatchEvent(rabbitTemplate: RabbitTemplate, event: ICodeWebhookEvent): Boolean {
        logger.debug("Webhook comming [${event.commitEventType}|${event.requestContent}]")
        var result = false
        try {
            logger.info("${event.traceId}|Dispatch the ${event.commitEventType} webhook event by MQ")
            val eventType = event::class.java.annotations.find { s -> s is Event } as Event
            rabbitTemplate.convertAndSend(eventType.exchange, eventType.routeKey, event) { message ->
                // 事件中的变量指定
                if (event.delayMills > 0) {
                    message.messageProperties.setHeader("x-delay", event.delayMills)
                } else if (eventType.delayMills > 0) { // 事件类型固化默认值
                    message.messageProperties.setHeader("x-delay", eventType.delayMills)
                }
                message
            }
            result = true
        } catch (e: Throwable) {
            logger.error("Fail to dispatch the event($event) by MQ", e)
        }
        return result
    }

    fun dispatchGithubEvent(rabbitTemplate: RabbitTemplate, event: GithubWebhookEvent): Boolean {
        logger.debug("Webhook comming [GITHUB|${event.githubWebhook.event}]")
        var result = false
        try {
            logger.info("Dispatch the GITHUB webhook event by MQ")
            val eventType = event::class.java.annotations.find { s -> s is Event } as Event
            rabbitTemplate.convertAndSend(eventType.exchange, eventType.routeKey, event) { message ->
                // 事件中的变量指定
                if (event.delayMills > 0) {
                    message.messageProperties.setHeader("x-delay", event.delayMills)
                } else if (eventType.delayMills > 0) { // 事件类型固化默认值
                    message.messageProperties.setHeader("x-delay", eventType.delayMills)
                }
                message
            }
            result = true
        } catch (e: Throwable) {
            logger.error("Fail to dispatch the event($event) by MQ", e)
        }
        return result
    }

    fun dispatchReplayEvent(rabbitTemplate: RabbitTemplate, event: ReplayWebhookEvent): Boolean {
        logger.debug("Webhook comming [replay|$event]")
        var result = false
        try {
            logger.info("Dispatch the replay webhook event by MQ")
            val eventType = event::class.java.annotations.find { s -> s is Event } as Event
            rabbitTemplate.convertAndSend(eventType.exchange, eventType.routeKey, event) { message ->
                // 事件中的变量指定
                if (event.delayMills > 0) {
                    message.messageProperties.setHeader("x-delay", event.delayMills)
                } else if (eventType.delayMills > 0) { // 事件类型固化默认值
                    message.messageProperties.setHeader("x-delay", eventType.delayMills)
                }
                message
            }
            result = true
        } catch (e: Throwable) {
            logger.error("Fail to dispatch the event($event) by MQ", e)
        }
        return result
    }

    private val logger = LoggerFactory.getLogger(CodeWebhookEventDispatcher::class.java)
}

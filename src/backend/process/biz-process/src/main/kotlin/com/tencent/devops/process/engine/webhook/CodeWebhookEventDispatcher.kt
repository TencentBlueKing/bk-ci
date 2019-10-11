package com.tencent.devops.process.engine.webhook

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.process.engine.pojo.event.commit.GithubWebhookEvent
import com.tencent.devops.process.engine.pojo.event.commit.ICodeWebhookEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

/**
 * @ Author     ：Royal Huang
 * @ Date       ：Created in 10:32 2019-08-08
 */

object CodeWebhookEventDispatcher {

    fun dispatchEvent(rabbitTemplate: RabbitTemplate, event: ICodeWebhookEvent): Boolean {
        logger.debug("Webhook comming [${event.commitEventType}|${event.requestContent}]")
        var result = false
        try {
            logger.info("Dispatch the ${event.commitEventType} webhook event by MQ")
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

    private val logger = LoggerFactory.getLogger(CodeWebhookEventDispatcher::class.java)
}

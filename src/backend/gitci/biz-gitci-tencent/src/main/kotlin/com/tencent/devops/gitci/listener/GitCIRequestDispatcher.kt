package com.tencent.devops.gitci.listener

import com.tencent.devops.common.event.annotation.Event
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

object GitCIRequestDispatcher {

    fun dispatch(rabbitTemplate: RabbitTemplate, event: GitCIRequestTriggerEvent) {
        try {
            logger.info("[${event.event}] Dispatch the event")
            val eventType = event::class.java.annotations.find { s -> s is Event } as Event
            rabbitTemplate.convertAndSend(eventType.exchange, eventType.routeKey, event)
        } catch (e: Throwable) {
            logger.error("Fail to dispatch the event($event)", e)
        }
    }

    private val logger = LoggerFactory.getLogger(GitCIRequestDispatcher::class.java)
}

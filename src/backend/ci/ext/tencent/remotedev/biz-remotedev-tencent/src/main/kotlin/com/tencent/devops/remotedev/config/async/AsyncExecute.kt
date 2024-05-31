package com.tencent.devops.remotedev.config.async

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.pojo.async.AsyncExecuteEventData
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import com.tencent.devops.common.event.annotation.Event

object AsyncExecute {
    fun dispatch(rabbitTemplate: RabbitTemplate, data: AsyncExecuteEventData) {
        dispatch(
            rabbitTemplate, AsyncExecuteEvent(
                eventStr = JsonUtil.toJson(data, false),
                type = data.toType()
            )
        )
    }

    private fun dispatch(rabbitTemplate: RabbitTemplate, event: AsyncExecuteEvent) {
        try {
            logger.info("AsyncExecuteDispatch|${event.type}|${event.eventStr}")
            val eventType = event::class.java.annotations.find { s -> s is Event } as Event
            rabbitTemplate.convertAndSend(eventType.exchange, eventType.routeKey, event)
        } catch (e: Throwable) {
            logger.error("AsyncExecuteDispatch|error:", e)
        }
    }

    private val logger = LoggerFactory.getLogger(AsyncExecute::class.java)
}
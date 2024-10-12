package com.tencent.devops.remotedev.config.async

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.pojo.async.AsyncExecuteEventData
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import com.tencent.devops.common.event.annotation.Event

object AsyncExecute {
    fun dispatch(rabbitTemplate: RabbitTemplate, data: AsyncExecuteEventData, errorLogTag: String? = null) {
        dispatch(
            rabbitTemplate = rabbitTemplate,
            event = AsyncExecuteEvent(
                eventStr = JsonUtil.toJson(data, false),
                type = data.toType()
            ),
                errorLogTag = errorLogTag
        )
    }

    private fun dispatch(rabbitTemplate: RabbitTemplate, event: AsyncExecuteEvent, errorLogTag: String? = null) {
        try {
            logger.info("AsyncExecuteDispatch|${event.type}|${event.eventStr}")
            val eventType = event::class.java.annotations.find { s -> s is Event } as Event
            rabbitTemplate.convertAndSend(eventType.exchange, eventType.routeKey, event)
        } catch (e: Throwable) {
            if (errorLogTag.isNullOrBlank()) {
                logger.error("AsyncExecuteDispatch|error:", e)
            } else {
                // 针对某些特殊的需要配置告警的场景添加
                logger.error("$errorLogTag|AsyncExecuteDispatch|error:", e)
            }
        }
    }

    private val logger = LoggerFactory.getLogger(AsyncExecute::class.java)
}

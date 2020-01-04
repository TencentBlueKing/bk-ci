package com.tencent.devops.common.websocket.dispatch

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.EventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.IPipelineRoutableEvent
import com.tencent.devops.common.websocket.dispatch.push.TransferPush
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.lang.Exception

class TransferDispatch (
    private val rabbitTemplate: RabbitTemplate
) : EventDispatcher<TransferPush> {
    companion object{
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun dispatch(vararg events: TransferPush) {
        try {
            events.forEach { event ->
                val eventType = event::class.java.annotations.find { s -> s is Event } as Event
                val routeKey = // 根据 routeKey+后缀 实现动态变换路由Key
                    if (event is IPipelineRoutableEvent && !event.routeKeySuffix.isNullOrBlank()) {
                        eventType.routeKey + event.routeKeySuffix
                    } else {
                        eventType.routeKey
                    }
                logger.info("[${eventType.exchange}|$routeKey|${event.userId}|${event.page}] dispatch the transfer event")
                rabbitTemplate.convertAndSend(eventType.exchange, routeKey, event) { message ->
                    when {
                        event.delayMills!! > 0 -> message.messageProperties.setHeader("x-delay", event.delayMills)
                        eventType.delayMills > 0 -> // 事件类型固化默认值
                            message.messageProperties.setHeader("x-delay", eventType.delayMills)
                    }
                    message
                }
            }
        } catch (e: Exception) {
            logger.error("Fail to dispatch the event($events)", e)
        }
    }
}
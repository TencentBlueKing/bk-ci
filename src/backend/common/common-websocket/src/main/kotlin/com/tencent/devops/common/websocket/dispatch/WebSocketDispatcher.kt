package com.tencent.devops.common.websocket.dispatch

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.EventDispatcher
import com.tencent.devops.common.websocket.dispatch.push.WebsocketPush
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

class WebSocketDispatcher(
    private val rabbitTemplate: RabbitTemplate
) : EventDispatcher<WebsocketPush> {

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun dispatch(vararg events: WebsocketPush) {
        try {
            events.forEach { event ->
                val eventType = event::class.java.annotations.find { s -> s is Event } as Event
                val routeKey = eventType.routeKey
                val mqMessage = event.buildMqMessage()
                if(mqMessage?.sessionList != null && mqMessage.sessionList!!.isNotEmpty()) {
                    event.buildNotifyMessage(mqMessage)
                    logger.info("[WebsocketDispatcher]:mqMessageType:${mqMessage.javaClass},mqMessage:$mqMessage")
                    rabbitTemplate.convertAndSend(eventType.exchange, routeKey, mqMessage) { message ->
                        if (eventType.delayMills > 0) { // 事件类型固化默认值
                            message.messageProperties.setHeader("x-delay", eventType.delayMills)
                        }
                        message
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Fail to dispatch the event($events)", e)
        }
    }
}
package com.tencent.devops.common.websocket.dispatch

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.EventDispatcher
import com.tencent.devops.common.websocket.dispatch.push.IWebsocketPush
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

class WebsocketPushDispatcher(
    private val rabbitTemplate: RabbitTemplate
) : EventDispatcher<IWebsocketPush> {

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun dispatch(vararg events: IWebsocketPush) {
        try {
            events.forEach { event ->
                val eventType = event::class.java.annotations.find { s -> s is Event } as Event
                val routeKey = eventType.routeKey
//                logger.info("WebsocketPushDispatcher: buildId:${event.buildId},pipelineId:${event.pipelineId},project:${event.projectId},type:${event.pushType.name}")
                if (event.isPushBySession() && event.isPushByPage()) {
                    val mqMessage = event.mqMessage()
                    logger.info("[WebsocketPushDispatcher]:mqMessageType:${mqMessage.javaClass},mqMessage:$mqMessage")
                    rabbitTemplate.convertAndSend(eventType.exchange, routeKey, mqMessage) { message ->
                        if (eventType.delayMills > 0) { // 事件类型固化默认值
                            message.messageProperties.setHeader("x-delay", eventType.delayMills)
                        }
                        message
                    }
                } else {
                    logger.info("WebsocketPushDispatcher--未命中,无需推送。 event:$event")
                }
            }
        } catch (e: Exception) {
            logger.error("Fail to dispatch the event($events)", e)
        }
    }

    fun dispatchBackup(vararg events: IWebsocketPush) {
        try {
            events.forEach { event ->
                val eventType = event::class.java.annotations.find { s -> s is Event } as Event
                val routeKey = eventType.routeKey
//                logger.info("WebsocketPushDispatcher: buildId:${event.buildId},pipelineId:${event.pipelineId},project:${event.projectId},type:${event.pushType.name}")
                if (event.isPushBySession() && event.isPushByPage()) {
                    event.buildMessage(event)
                    val sendMessage = event.buildSendMessage()
                    rabbitTemplate.convertAndSend(eventType.exchange, routeKey, sendMessage) { message ->
                        if (eventType.delayMills > 0) { // 事件类型固化默认值
                            message.messageProperties.setHeader("x-delay", eventType.delayMills)
                        }
                        message
                    }
                } else {
                    logger.info("WebsocketPushDispatcher--未命中,无需推送。 event:$event")
                }
            }
        } catch (e: Exception) {
            logger.error("Fail to dispatch the event($events)", e)
        }
    }
}
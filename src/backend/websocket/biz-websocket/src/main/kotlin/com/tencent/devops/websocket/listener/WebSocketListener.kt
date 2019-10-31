package com.tencent.devops.websocket.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class WebSocketListener @Autowired constructor(
        val objectMapper: ObjectMapper,
        val messagingTemplate: SimpMessagingTemplate
): Listener<SendMessage> {

    companion object{
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun execute(event: SendMessage) {
        logger.info("WebSocketListener: user:${event.userId},page:${event.page},notifyPost:${event.notifyPost},sessionList:${event.sessionList}")
        try {
            val sessionList = event.sessionList
            if (sessionList != null && sessionList.isNotEmpty()) {
                sessionList.forEach { session ->
                    messagingTemplate!!.convertAndSend(
                            "/topic/bk/notify/$session",
                            objectMapper.writeValueAsString(event.notifyPost))
                }
            }
        } catch (ex: Exception) {
            logger.error("webSocketListener error", ex)
        }
    }
}
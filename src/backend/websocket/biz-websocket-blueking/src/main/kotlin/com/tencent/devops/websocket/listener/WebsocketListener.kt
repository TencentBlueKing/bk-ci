package com.tencent.devops.websocket.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.MqMessage
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.websocket.message.AmdWebsocketMessage
import com.tencent.devops.websocket.message.CodeccNotifyMessage
import com.tencent.devops.websocket.message.DetailWebsocketMessage
import com.tencent.devops.websocket.message.HistoryWebsocketMessage
import com.tencent.devops.websocket.message.HookWebsocketMessage
import com.tencent.devops.websocket.message.ISendMessage
import com.tencent.devops.websocket.message.StatusWebsocketMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class WebsocketListener @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val messagingTemplate: SimpMessagingTemplate,
    private val redisOperation: RedisOperation
) : Listener<MqMessage> {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun execute(event: MqMessage) {
        logger.info("WebSocketListener: type:${event.pushType.name},page:${event.page},notifyPost:${event.notifyPost}")
        try {
            val sendMessage = getSendMessage(event.pushType)
            if (sendMessage != null) {
                val message = sendMessage.buildMessageInfo(event)
                if (message != null) {
                    sendMessage.sendWebsocketMessage(message)
                }
            } else {
                logger.error("[webSocketListener error] mqMessage:$event")
            }
        } catch (ex: Exception) {
            logger.error("webSocketListener error", ex)
        }
    }

    private fun getSendMessage(type: WebSocketType): ISendMessage? {
        when (type) {
            WebSocketType.CODECC -> {
                return CodeccNotifyMessage(messagingTemplate, objectMapper, redisOperation)
            }
            WebSocketType.WEBHOOK -> {
                return HookWebsocketMessage(messagingTemplate, objectMapper, redisOperation)
            }
            WebSocketType.STATUS -> {
                return StatusWebsocketMessage(messagingTemplate, objectMapper, client, redisOperation)
            }
            WebSocketType.DETAIL -> {
                return DetailWebsocketMessage(messagingTemplate, objectMapper, client, redisOperation)
            }
            WebSocketType.HISTORY -> {
                return HistoryWebsocketMessage(messagingTemplate, redisOperation, objectMapper)
            }
            WebSocketType.STORE -> {
                return AmdWebsocketMessage(messagingTemplate, redisOperation, objectMapper, client)
            }
        }
        return null
    }
}
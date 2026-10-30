package com.tencent.devops.websocket.handler

import com.fasterxml.jackson.databind.node.ObjectNode
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.websocket.servcie.LegacySessionRegistry
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor

class LegacyIdentityInterceptor : ChannelInterceptor {
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message
        val attributes = headers.sessionAttributes.orEmpty()
        val sessionId = attributes[LegacySessionRegistry.SESSION_ATTRIBUTE] as? String ?: return message
        val userId = attributes[LegacySessionRegistry.USER_ATTRIBUTE] as? String ?: return null
        if (headers.command == StompCommand.SUBSCRIBE && headers.destination?.startsWith("/topic/bk/notify/") == true) {
            require(headers.destination == "/topic/bk/notify/$sessionId") { "Invalid websocket subscription owner" }
        }
        if (headers.command == StompCommand.SEND && headers.destination in setOf(
                "/app/changePage", "/app/loginOut", "/app/clearUserSession"
            )
        ) {
            val bytes = message.payload as? ByteArray ?: return null
            require(bytes.size <= 65536) { "Websocket command is too large" }
            val mapper = JsonUtil.getObjectMapper(false)
            val body = mapper.readTree(bytes) as? ObjectNode ?: return null
            body.put("userId", userId)
            body.put("sessionId", sessionId)
            return MessageBuilder.createMessage(mapper.writeValueAsBytes(body), message.headers)
        }
        return message
    }
}

package com.tencent.devops.websocket.handler

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.support.ChannelInterceptorAdapter
import org.springframework.stereotype.Component
import org.springframework.messaging.simp.stomp.StompHeaderAccessor

@Component
class ConnectChannelInterceptor @Autowired constructor(
    val redisOperation: RedisOperation
) : ChannelInterceptorAdapter() {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun postSend(message: Message<*>?, channel: MessageChannel?, sent: Boolean) {
        val sha = StompHeaderAccessor.wrap(message)
        val userId: String = sha.getHeader(AUTH_HEADER_DEVOPS_USER_ID) as String
        if (sha.command == null) {
            return
        }
        when (sha.command) {
            StompCommand.CONNECT -> connect(userId)
            StompCommand.DISCONNECT -> disconnect(userId)
        }
    }

    private fun connect(userId: String) {
        logger.info("[ConnectChannelInterceptor]:connect success, userId:$userId")
    }

    private fun disconnect(userId: String) {
        logger.info("[ConnectChannelInterceptor]:disconnect, userId:$userId")
    }
}
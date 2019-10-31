package com.tencent.devops.websocket.handler

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class BKHandshakeInterceptor @Autowired constructor(
    val redisOperation: RedisOperation
) : HandshakeInterceptor {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun afterHandshake(
        request: ServerHttpRequest?,
        response: ServerHttpResponse?,
        wsHandler: WebSocketHandler?,
        exception: Exception?
    ) {
        if (request is ServletServerHttpRequest) {
            val sessionId = request.servletRequest.getParameter("sessionId")
            var userId = request.servletRequest.getHeader(AUTH_HEADER_DEVOPS_USER_ID)
            if (userId != null && sessionId != null) {
                RedisUtlis.writeSessionIdByRedis(redisOperation, userId, sessionId)
                logger.info(
                    "[WebSocket]-[$userId]-[$sessionId]-连接成功,redisData:${RedisUtlis.getSessionIdByUserId(
                        redisOperation,
                        userId
                    )}"
                )
            }
        }
    }

    override fun beforeHandshake(
        request: ServerHttpRequest?,
        response: ServerHttpResponse?,
        wsHandler: WebSocketHandler?,
        attributes: MutableMap<String, Any>?
    ): Boolean {
        return true
    }
}
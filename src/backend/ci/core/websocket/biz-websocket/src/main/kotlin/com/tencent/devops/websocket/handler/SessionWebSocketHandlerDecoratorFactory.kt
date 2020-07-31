package com.tencent.devops.websocket.handler

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.websocket.servcie.WebsocketService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory

class SessionWebSocketHandlerDecoratorFactory @Autowired constructor(
    val websocketService: WebsocketService,
    val redisOperation: RedisOperation
) : WebSocketHandlerDecoratorFactory {

    override fun decorate(handler: WebSocketHandler?): WebSocketHandler {
        return SessionHandler(handler, websocketService, redisOperation)
    }
}
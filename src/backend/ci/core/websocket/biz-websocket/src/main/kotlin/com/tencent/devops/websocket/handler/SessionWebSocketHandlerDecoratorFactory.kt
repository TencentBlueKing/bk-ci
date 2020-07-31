package com.tencent.devops.websocket.handler

import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory

class SessionWebSocketHandlerDecoratorFactory() : WebSocketHandlerDecoratorFactory {

	override fun decorate(handler: WebSocketHandler?): WebSocketHandler {
		return SessionHandler(handler)
	}
}
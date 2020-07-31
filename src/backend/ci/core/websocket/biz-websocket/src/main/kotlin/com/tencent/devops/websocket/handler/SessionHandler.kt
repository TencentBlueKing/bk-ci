package com.tencent.devops.websocket.handler

import org.slf4j.LoggerFactory
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.WebSocketHandlerDecorator

class SessionHandler(delegate: WebSocketHandler?) : WebSocketHandlerDecorator(delegate) {

	// 链接关闭记录去除session
	override fun afterConnectionClosed(session: WebSocketSession?, closeStatus: CloseStatus?) {
		val sessionId = session?.id
		val uri = session?.uri
		val remoteId = session?.remoteAddress
		logger.info("connection closed success: |$sessionId| $uri | $remoteId ")
		super.afterConnectionClosed(session, closeStatus)
	}

	override fun afterConnectionEstablished(session: WebSocketSession?) {
		val sessionId = session?.id
		val uri = session?.uri
		val remoteId = session?.remoteAddress
		logger.info("connection success: |$sessionId| $uri | $remoteId ")
		super.afterConnectionEstablished(session)
	}

	companion object {
		val logger = LoggerFactory.getLogger(this::class.java)
	}
}
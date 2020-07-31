package com.tencent.devops.websocket.handler

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.utils.RedisUtlis
import com.tencent.devops.websocket.servcie.WebsocketService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.WebSocketHandlerDecorator

class SessionHandler @Autowired constructor(
		delegate: WebSocketHandler?,
		val websocketService: WebsocketService,
		val redisOperation: RedisOperation
) : WebSocketHandlerDecorator(delegate) {

	// 链接关闭记录去除session
	override fun afterConnectionClosed(session: WebSocketSession?, closeStatus: CloseStatus?) {
		val sessionId = session?.id
		val uri = session?.uri
		val remoteId = session?.remoteAddress
		logger.info("connection closed success: |$sessionId| $uri | $remoteId ")

		val page = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId!!)
		val userId = RedisUtlis.getUserBySession(redisOperation, sessionId)
		logger.info("connection closed user[$userId] page[$page], session[$sessionId] clear")
		websocketService.clearSession(userId!!, sessionId)
		websocketService.removeCacheSession(sessionId)
		websocketService.loginOut(userId, sessionId, page)

		super.afterConnectionClosed(session, closeStatus)
	}

	override fun afterConnectionEstablished(session: WebSocketSession?) {
		val sessionId = session?.id
		val uri = session?.uri
		val remoteId = session?.remoteAddress
		if(session == null) {
			logger.warn("connection warm: session is empty, $uri")
			return super.afterConnectionEstablished(session)
		}
		websocketService.addCacheSession(sessionId!!)
		logger.info("connection success: |$sessionId| $uri | $remoteId ")
		super.afterConnectionEstablished(session)
	}

	companion object {
		val logger = LoggerFactory.getLogger(this::class.java)
	}
}
package com.tencent.devops.websocket.handler

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.utils.RedisUtlis
import com.tencent.devops.websocket.servcie.WebsocketService
import com.tencent.devops.websocket.utils.HostUtils
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
        val uri = session?.uri
        if (closeStatus?.code != CloseStatus.NORMAL.code && closeStatus?.code != CloseStatus.PROTOCOL_ERROR.code) {
            logger.warn("websocket close not normal, Status[$closeStatus] uri[${session?.uri}] remoteIp[${session?.remoteAddress}]")
        }
        val sessionId = HostUtils.getRealSession(session?.uri?.query)
        if (sessionId.isNullOrEmpty()) {
            logger.warn("connection closed can not find sessionId, $uri| ${session?.remoteAddress}")
            super.afterConnectionClosed(session, closeStatus)
        }
        val page = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId!!)
        val userId = RedisUtlis.getUserBySession(redisOperation, sessionId)
        if (userId.isNullOrEmpty()) {
            logger.warn("connection closed can not find userId, $uri| ${session?.remoteAddress}| $sessionId")
            super.afterConnectionClosed(session, closeStatus)
        }
        logger.info("connection closed closeStatus[$closeStatus] user[$userId] page[$page], session[$sessionId]")
        websocketService.clearAllBySession(userId!!, sessionId)

        super.afterConnectionClosed(session, closeStatus)
    }

    override fun afterConnectionEstablished(session: WebSocketSession?) {
        val uri = session?.uri
        val remoteId = session?.remoteAddress
        if (session == null) {
            logger.warn("connection warm: session is empty, $uri")
            return super.afterConnectionEstablished(session)
        }
        val sessionId = uri?.query?.substringAfter("sessionId=")
        val webUser = session.handshakeHeaders[AUTH_HEADER_DEVOPS_USER_ID]
        websocketService.addCacheSession(sessionId!!)
        logger.info("connection success: |$sessionId| $uri | $remoteId | $webUser ")
        super.afterConnectionEstablished(session)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
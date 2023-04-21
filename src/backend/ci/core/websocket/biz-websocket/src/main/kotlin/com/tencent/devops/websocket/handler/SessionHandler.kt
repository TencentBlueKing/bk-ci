/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.websocket.handler

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.utils.WsRedisUtils
import com.tencent.devops.websocket.servcie.WebsocketService
import com.tencent.devops.websocket.utils.HostUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.WebSocketHandlerDecorator

class SessionHandler @Autowired constructor(
    delegate: WebSocketHandler,
    private val websocketService: WebsocketService,
    private val redisOperation: RedisOperation
) : WebSocketHandlerDecorator(delegate) {

    // 链接关闭记录去除session
    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        val uri = session.uri
        if (closeStatus.code != CloseStatus.NORMAL.code && closeStatus.code != CloseStatus.PROTOCOL_ERROR.code) {
            logger.warn("websocket close abnormal, [$closeStatus] [${session.uri}] [${session.remoteAddress}]")
        }
        val sessionId = HostUtils.getRealSession(session.uri?.query)
        if (sessionId.isNullOrEmpty()) {
            logger.warn("connection closed can not find sessionId, $uri| ${session.remoteAddress}")
            super.afterConnectionClosed(session, closeStatus)
        }
        val page = WsRedisUtils.getPageFromSessionPageBySession(redisOperation, sessionId!!)
        val userId = WsRedisUtils.getUserBySession(redisOperation, sessionId)
        if (userId.isNullOrEmpty()) {
            logger.warn("connection closed can not find userId, $uri| ${session.remoteAddress}| $sessionId")
            super.afterConnectionClosed(session, closeStatus)
        } else {
            logger.info("connection closed closeStatus[$closeStatus] user[$userId] page[$page], session[$sessionId]")
            websocketService.clearAllBySession(userId, sessionId)
        }

        super.afterConnectionClosed(session, closeStatus)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val uri = session.uri
        val remoteId = session.remoteAddress
        val sessionId = uri?.query?.split("&")
            ?.firstOrNull { it.contains("sessionId") }?.substringAfter("sessionId=")
        val webUser = session.handshakeHeaders[AUTH_HEADER_DEVOPS_USER_ID]
        websocketService.addCacheSession(sessionId!!)
        logger.info("connection success: |$sessionId| $uri | $remoteId | $webUser ")
        super.afterConnectionEstablished(session)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SessionHandler::class.java)
    }
}

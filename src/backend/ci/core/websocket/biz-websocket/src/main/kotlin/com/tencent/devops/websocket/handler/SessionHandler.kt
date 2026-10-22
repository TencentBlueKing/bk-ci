/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.websocket.servcie.LegacySessionRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.WebSocketHandlerDecorator

class SessionHandler @Autowired constructor(
    delegate: WebSocketHandler,
    private val redisOperation: RedisOperation
) : WebSocketHandlerDecorator(delegate) {

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        try {
            val sessionId = session.attributes[LegacySessionRegistry.SESSION_ATTRIBUTE] as? String
            if (sessionId != null) {
                SpringContextUtil.getBean(LegacySessionRegistry::class.java).disconnected(sessionId, session.id)
            }
        } catch (exception: Exception) {
            logger.warn("Unable to clean closed websocket session", exception)
        } finally {
            super.afterConnectionClosed(session, closeStatus)
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val sessionId = session.attributes[LegacySessionRegistry.SESSION_ATTRIBUTE] as? String
        val userId = session.attributes[LegacySessionRegistry.USER_ATTRIBUTE] as? String
        if (sessionId.isNullOrBlank() || userId.isNullOrBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }
        try {
            SpringContextUtil.getBean(LegacySessionRegistry::class.java).connected(sessionId, userId, session)
            super.afterConnectionEstablished(session)
        } catch (exception: Exception) {
            logger.warn("Unable to establish websocket session", exception)
            session.close(CloseStatus.SERVER_ERROR)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SessionHandler::class.java)
    }
}

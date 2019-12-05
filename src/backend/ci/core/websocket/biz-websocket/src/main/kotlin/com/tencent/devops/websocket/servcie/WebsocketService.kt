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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.websocket.servcie

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.utils.RedisUtlis
import com.tencent.devops.websocket.utils.PageUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WebsocketService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val projectProxyService: ProjectProxyService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    // 用户切换页面，需调整sessionId-page,page-sessionIdList两个map
    fun changePage(userId: String, sessionId: String, newPage: String, projectId: String) {
        if (!projectProxyService.checkProject(projectId, userId)) {
            return
        }
        val redisLock = lockUser(sessionId)
        try {
            redisLock.lock()
            if (!checkParams(userId, sessionId)) {
                logger.warn("changPage checkFail: userId:$userId,sessionId:$sessionId")
                return
            }

            val normalPage = PageUtils.buildNormalPage(newPage)
            logger.info("WebsocketService-changePage:user:$userId,sessionId:$sessionId,newPage:$newPage,normalPage:$normalPage")
            val existsSessionId = RedisUtlis.getSessionIdByUserId(redisOperation, userId)
            if (existsSessionId == null) {
                RedisUtlis.writeSessionIdByRedis(redisOperation, userId, sessionId)
            }

            val oldPage = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId)
            RedisUtlis.refreshSessionPage(redisOperation, sessionId, normalPage)
            if (oldPage != null) {
                RedisUtlis.cleanPageSessionBySessionId(redisOperation, oldPage, sessionId)
            }
            RedisUtlis.refreshPageSession(redisOperation, sessionId, normalPage)
            logger.info(
                "userSession[user:$userId,sessionId:${RedisUtlis.getSessionIdByUserId(
                    redisOperation,
                    userId
                )}}]"
            )
            logger.info(
                "pageSession[page:$newPage,sessionId:${RedisUtlis.getSessionListFormPageSessionByPage(
                    redisOperation,
                    normalPage
                )}]"
            )
            logger.info("sessionPage[session:$sessionId,page:$normalPage]")
        } finally {
            redisLock.unlock()
        }
    }

    fun loginOut(userId: String, sessionId: String, oldPage: String?) {
        if (!checkParams(userId, sessionId)) {
            logger.warn("loginOut checkFail: [userId:$userId,sessionId:$sessionId")
            return
        }
        val redisLock = lockUser(sessionId)
        try {
            redisLock.lock()
            logger.info("WebsocketService-loginOut:user:$userId,sessionId:$sessionId")
            val page = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId)
            if (!oldPage.isNullOrEmpty() && page != oldPage) {
                logger.warn("loginOut error: oldPage:$oldPage, redisPage:$page, userId:$userId, sessionId:$sessionId")
            }
//            RedisUtlis.cleanUserSessionBySessionId(redisOperation, userId, sessionId)
            RedisUtlis.cleanSessionPageBySessionId(redisOperation, sessionId)
//            RedisUtlis.cleanSessionTimeOutBySession(redisOperation, sessionId)
            if (oldPage != null) {
                RedisUtlis.cleanPageSessionBySessionId(redisOperation, oldPage, sessionId)
            } else if (page != null) {
                RedisUtlis.cleanPageSessionBySessionId(redisOperation, page, sessionId)
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun clearUserSession(userId: String, sessionId: String) {
        val redisLock = lockUser(sessionId)
        try {
            redisLock.lock()
            logger.info("clearUserSession:user:$userId,sessionId:$sessionId")
            logger.info("before clearUserSession:${RedisUtlis.getSessionIdByUserId(redisOperation, userId)}")
            RedisUtlis.deleteSigelSessionByUser(redisOperation, userId, sessionId)
            RedisUtlis.cleanSessionTimeOutBySession(redisOperation, sessionId)
            logger.info("after clearUserSession:${RedisUtlis.getSessionIdByUserId(redisOperation, userId)}")
        } finally {
            redisLock.unlock()
        }
    }

    private fun checkParams(userId: String, sessionId: String): Boolean {
        if (userId == null) {
            return false
        }

        if (sessionId == null) {
            return false
        }
        return true
    }

    private fun lockUser(sessionId: String): RedisLock {
        return RedisLock(redisOperation, "websocket:changeStatus:$sessionId", 10L)
    }
}
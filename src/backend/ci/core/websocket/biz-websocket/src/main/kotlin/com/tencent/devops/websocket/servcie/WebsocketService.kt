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

package com.tencent.devops.websocket.servcie

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.utils.WsRedisUtils
import com.tencent.devops.websocket.event.ClearSessionEvent
import com.tencent.devops.websocket.pojo.ChangePageDTO
import com.tencent.devops.websocket.utils.WebsocketPageUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
@Suppress("ALL")
class WebsocketService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val transferDispatch: SampleEventDispatcher,
    private val projectProxyService: ProjectProxyService,
    private val sessionRegistry: LegacySessionRegistry
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WebsocketService::class.java)
    }

    @Value("\${session.maxCount:50}")
    private val cacheMaxSession: Int = 50

    private val cacheSessionList = ConcurrentHashMap.newKeySet<String>()

    private val longSessionList = ConcurrentHashMap.newKeySet<String>()

    // 用户切换页面，需调整sessionId-page,page-sessionIdList两个map
    fun changePage(changePage: ChangePageDTO) {
        if (changePage.showProjectList && !changePage.projectId.isNullOrBlank()) {
            if (!projectProxyService.checkProject(changePage.projectId, changePage.userId)) {
                return
            }
        }
        if (!checkParams(changePage.userId, changePage.sessionId)) {
            logger.warn("changPage checkFail: userId:$changePage.userId,sessionId:$changePage.sessionId")
            return
        }
        val redisLock = lockUser(changePage.sessionId)

        redisLock.use {
            redisLock.lock()

            val normalPage = WebsocketPageUtils.buildNormalPage(changePage.page)
            WsRedisUtils.writeSessionIdByRedis(redisOperation, changePage.userId, changePage.sessionId)

            val oldPage = WsRedisUtils.getPageFromSessionPageBySession(redisOperation, changePage.sessionId)
            WsRedisUtils.refreshSessionPage(redisOperation, changePage.sessionId, normalPage)
            if (oldPage != null) {
                WsRedisUtils.cleanPageSessionBySessionId(redisOperation, oldPage, changePage.sessionId)
            }
            WsRedisUtils.refreshPageSession(redisOperation, changePage.sessionId, normalPage)
            sessionRegistry.pageChanged(changePage.sessionId, normalPage)

            logger.info("sessionPage[session:$changePage.sessionId,page:$normalPage]")
        }
    }

    fun loginOut(
        userId: String,
        sessionId: String,
        oldPage: String?,
        transferData: Map<String, Any>? = emptyMap()
    ) {
        if (!checkParams(userId, sessionId)) {
            logger.warn("loginOut checkFail: [userId:$userId,sessionId:$sessionId")
            return
        }
        val redisLock = lockUser(sessionId)
        try {
            redisLock.lock()
            logger.info("WebsocketService loginOut:user:$userId,sessionId:$sessionId")
            val redisPage = WsRedisUtils.getPageFromSessionPageBySession(redisOperation, sessionId)
            var clearPage = oldPage
            if (!oldPage.isNullOrEmpty() && redisPage != oldPage) {
                clearPage = WebsocketPageUtils.buildNormalPage(oldPage)
            }

            if (clearPage != null) {
                WsRedisUtils.cleanPageSessionBySessionId(redisOperation, clearPage, sessionId)
            }
            if (clearPage == null || clearPage == redisPage) {
                WsRedisUtils.cleanSessionPageBySessionId(redisOperation, sessionId)
                redisPage?.let { WsRedisUtils.cleanPageSessionBySessionId(redisOperation, it, sessionId) }
                sessionRegistry.pageChanged(sessionId, null)
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun clearUserSession(
        userId: String,
        sessionId: String,
        transferData: Map<String, Any>?
    ) {
        val redisLock = lockUser(sessionId)
        try {
            redisLock.lock()
            if (sessionRegistry.isManaged(sessionId)) return
            if (WsRedisUtils.getUserBySession(redisOperation, sessionId)?.let { it != userId } == true) return
            logger.info("clearUserSession:user:$userId,sessionId:$sessionId")
            WsRedisUtils.deleteUserSessionBySession(redisOperation, sessionId)
            WsRedisUtils.cleanUserSessionBySessionId(redisOperation, userId, sessionId)
            removeCacheSession(sessionId)
        } finally {
            redisLock.unlock()
        }
    }

    fun clearAllBySession(
        userId: String,
        sessionId: String
    ): Result<Boolean> {
        logger.info("clearSession| $userId| $sessionId")
        if (sessionRegistry.isManaged(sessionId)) return Result(true)
        if (WsRedisUtils.getUserBySession(redisOperation, sessionId)?.let { it != userId } == true) return Result(false)
        val page = WsRedisUtils.getPageFromSessionPageBySession(redisOperation, sessionId)
        clearUserSession(userId, sessionId, null)
        if (page != null) {
            logger.info("$userId| $sessionId|$page clear when disconnection")
            loginOut(userId, sessionId, page)
        }
        if (!isCacheSession(sessionId)) {
            transferDispatch.dispatch(
                ClearSessionEvent(
                    userId = userId,
                    sessionId = sessionId,
                    page = null,
                    transferData = mutableMapOf()
                )
            )
        }
        return Result(true)
    }

    fun addCacheSession(sessionId: String) {
        if (cacheSessionList.contains(sessionId)) {
            logger.warn("this session[$sessionId] already in cacheSession")
            return
        }
        cacheSessionList.add(sessionId)
    }

    // 清楚实例内部缓存的session
    fun removeCacheSession(sessionId: String) {
        cacheSessionList.remove(sessionId)
    }

    // 判断获取到的session是否由该实例持有,只有持有了该实例才能做push动作
    fun isCacheSession(sessionId: String): Boolean {
        return sessionRegistry.isLocal(sessionId) || cacheSessionList.contains(sessionId)
    }

    fun createLongSessionPage(page: String) {
        if (longSessionList.size < 1000) longSessionList.add(page)
    }

    fun getLongSessionPage(): Set<String> = longSessionList.toSet()

    fun clearLongSessionPage() {
        longSessionList.clear()
    }

    @Deprecated("Session expiry is managed by LegacySessionRegistry")
    fun createTimeoutSession(sessionId: String, userId: String) = Unit

    fun getMaxSession(): Int? {
        return cacheMaxSession
    }

    private fun checkParams(userId: String?, sessionId: String?): Boolean {
        if (userId.isNullOrBlank()) {
            return false
        }

        if (sessionId.isNullOrBlank()) {
            return false
        }
        return true
    }

    private fun lockUser(sessionId: String): RedisLock {
        return RedisLock(redisOperation, "websocket:changeStatus:$sessionId", 10L)
    }
}

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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.TransferDispatch
import com.tencent.devops.websocket.keys.WebsocketKeys
import com.tencent.devops.common.websocket.utils.RedisUtlis
import com.tencent.devops.common.websocket.utils.RedisUtlis.cleanUserSessionBySessionId
import com.tencent.devops.websocket.event.ChangePageTransferEvent
import com.tencent.devops.websocket.event.ClearSessionEvent
import com.tencent.devops.websocket.event.ClearUserSessionTransferEvent
import com.tencent.devops.websocket.event.LoginOutTransferEvent
import com.tencent.devops.websocket.utils.PageUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class WebsocketService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val transferDispatch: TransferDispatch,
    private val client: Client,
    private val projectProxyService: ProjectProxyService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    @Value("\${transferData:false}")
    private val needTransfer: Boolean = false

    @Value("\${session.timeout:5}")
    private val sessionTimeOut: Long? = null

    @Value("\${session.maxCount:50}")
    private val cacheMaxSession: Int? = null

    private val cacheSessionList = mutableListOf<String>()

    private val longSessionList = mutableSetOf<String>()

    // 用户切换页面，需调整sessionId-page,page-sessionIdList两个map
    fun changePage(
        userId: String,
        sessionId: String,
        newPage: String,
        projectId: String,
        needCheckProject: Boolean,
        transferData: Map<String, Any>? = emptyMap()
    ) {
        if (needCheckProject && projectId.isNotEmpty()) {
            if (!projectProxyService.checkProject(projectId, userId)) {
                return
            }
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

            logger.info("sessionPage[session:$sessionId,page:$normalPage]")
            if (needTransfer && transferData!!.isNotEmpty()) {
                transferDispatch.dispatch(
                    ChangePageTransferEvent(
                        userId = userId,
                        page = newPage,
                        transferData = transferData
                    )
                )
            }
        } finally {
            redisLock.unlock()
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
            val redisPage = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId)
            var clearPage = oldPage
            if (!oldPage.isNullOrEmpty() && redisPage != oldPage) {
                logger.warn("loginOut error: oldPage:$oldPage, redisPage:$redisPage, userId:$userId, sessionId:$sessionId")
                clearPage = PageUtils.buildNormalPage(oldPage!!)
            }

            RedisUtlis.cleanSessionPageBySessionId(redisOperation, sessionId)
            if (clearPage != null) {
                RedisUtlis.cleanPageSessionBySessionId(redisOperation, clearPage, sessionId)
            } else if (redisPage != null) {
                RedisUtlis.cleanPageSessionBySessionId(redisOperation, redisPage, sessionId)
            }
            cleanUserSessionBySessionId(redisOperation, userId, sessionId)
            if (needTransfer && transferData!!.isNotEmpty()) {
                transferDispatch.dispatch(
                    LoginOutTransferEvent(
                        userId = userId,
                        page = oldPage,
                        transferData = transferData
                    )
                )
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun clearUserSession(userId: String, sessionId: String, transferData: Map<String, Any>?) {
        val redisLock = lockUser(sessionId)
        try {
            redisLock.lock()
            logger.info("clearUserSession:user:$userId,sessionId:$sessionId")
            RedisUtlis.deleteUserSessionBySession(redisOperation, sessionId)
            RedisUtlis.deleteSigelSessionByUser(redisOperation, userId, sessionId)
//            RedisUtlis.cleanSessionTimeOutBySession(redisOperation, sessionId)
            removeCacheSession(sessionId)
            if (needTransfer && transferData!!.isNotEmpty()) {
                transferDispatch.dispatch(
                    ClearUserSessionTransferEvent(
                        userId = userId,
                        page = "",
                        transferData = transferData
                    )
                )
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun clearAllBySession(userId: String, sessionId: String): Result<Boolean> {
        logger.info("clearSession| $userId| $sessionId")
        val page = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId)
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
        if (cacheSessionList.contains(sessionId)) {
            logger.debug("sessionId[$sessionId] is in this host")
            return true
        }
        return false
    }

    fun createLongSessionPage(page: String) {
        longSessionList.add(page)
    }

    fun getLongSessionPage(): Set<String> {
        return longSessionList
    }

    fun clearLongSessionPage() {
        longSessionList.clear()
    }

    fun createTimeoutSession(sessionId: String, userId: String) {
        val timeout = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(sessionTimeOut!!)
        val redisData = "$sessionId#$userId&$timeout"
        // hash后对1000取模，将数据打散到1000个桶内
        var bucket = redisData.hashCode().rem(WebsocketKeys.REDIS_MO)
        if (bucket < 0) bucket *= -1
        val redisHashKey = WebsocketKeys.HASH_USER_TIMEOUT_REDIS_KEY + bucket
        logger.info("redis hash sessionId[$sessionId] userId[$userId] redisHashKey[$redisHashKey]")
        var timeoutData = redisOperation.get(redisHashKey)
        if (timeoutData == null) {
            redisOperation.set(redisHashKey, redisData, null, true)
        } else {
            timeoutData = "$timeoutData,$redisData"
            redisOperation.set(redisHashKey, timeoutData, null, true)
        }
    }

    fun getMaxSession(): Int? {
        return cacheMaxSession
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
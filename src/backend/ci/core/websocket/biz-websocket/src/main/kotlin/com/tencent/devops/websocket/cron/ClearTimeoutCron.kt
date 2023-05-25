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

package com.tencent.devops.websocket.cron

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.websocket.dispatch.TransferDispatch
import com.tencent.devops.common.websocket.utils.WsRedisUtils
import com.tencent.devops.common.websocket.utils.WsRedisUtils.cleanPageSessionByPage
import com.tencent.devops.websocket.event.ClearSessionEvent
import com.tencent.devops.websocket.keys.WebsocketKeys
import com.tencent.devops.websocket.lock.WebsocketCronLock
import com.tencent.devops.websocket.servcie.WebsocketService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.LinkedList

@Component
class ClearTimeoutCron(
    private val redisOperation: RedisOperation,
    private val websocketService: WebsocketService,
    private val transferDispatch: TransferDispatch
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ClearTimeoutCron::class.java)
    }

    /**
     * 每分钟一次，计算session是否已经超时，若超时，剔除该session关联的所有websocket redis信息。
     */
    @Scheduled(cron = "0 */30 * * * ?")
    fun newClearTimeoutCache() {
        longSessionLog()
        logger.info("websocket cron get redis lock")
        val websocketCronLock = WebsocketCronLock(redisOperation)
        try {
            websocketCronLock.lock()
            logger.info("websocket cron get redis lock")
            clearTimeoutSession()
        } finally {
            websocketCronLock.unlock()
            logger.info("websocket cron unlock")
        }
    }

    private fun longSessionLog() {
        val longSessionList = websocketService.getLongSessionPage()
        logger.warn("this page sessionSize more ${websocketService.getMaxSession()}, $longSessionList")
        longSessionList.forEach {
            cleanPageSessionByPage(redisOperation, it)
            logger.warn("this page[$it] outSize, delete page session")
        }
        websocketService.clearLongSessionPage()
    }

    @Suppress("ALL")
    private fun clearTimeoutSession() {
        val nowTime = System.currentTimeMillis()
        logger.info("start clear Session by Timer")
        for (bucket in 0..WebsocketKeys.REDIS_MO) {
            val redisData = redisOperation.get(WebsocketKeys.HASH_USER_TIMEOUT_REDIS_KEY + bucket)
            if (!redisData.isNullOrBlank()) {
                val redisDataArray = redisData.split(",")
                logger.info("websocket timer $bucket, data size:${redisDataArray.size}")
                val newRedisData = LinkedList<String>()
                redisDataArray.forEach {
                    try {
                        val timeout: Long = it.substringAfter("&").toLong()
                        val userId = it.substringAfter("#").substringBefore("&")
                        val sessionId = it.substringBefore("#")
                        if (nowTime > timeout) {
                            logger.info("websocket timer timeout $bucket|$it|$userId|$sessionId")
                            val sessionPage = WsRedisUtils.getPageFromSessionPageBySession(redisOperation, sessionId)
                            WsRedisUtils.cleanSessionPageBySessionId(redisOperation, sessionId)
                            if (sessionPage != null) {
                                WsRedisUtils.cleanPageSessionBySessionId(redisOperation, sessionPage, sessionId)
                                WsRedisUtils.cleanUserSessionBySessionId(redisOperation, userId, sessionId)
                            } else {
                                // sessionId没有匹配到页面且超时, 需把session从userId-sessionId内删除
                                WsRedisUtils.cleanUserSessionBySessionId(redisOperation, userId, sessionId)
                            }
                            // 如果不在本实例，下发到mq,供其他实例删除对应实例维持的session
                            if (websocketService.isCacheSession(sessionId)) {
                                websocketService.removeCacheSession(sessionId)
                            } else {
                                clearSessionByMq(userId, sessionId)
                            }
                        } else {
                            newRedisData.add(it)
                        }
                    } catch (e: Exception) {
                        logger.warn("fail msg: ${e.message}")
                    }
                }
                if (newRedisData.isNotEmpty()) {
                    logger.info("websocket timer reset $bucket, data size: ${newRedisData.size}")
                    redisOperation.set(
                        WebsocketKeys.HASH_USER_TIMEOUT_REDIS_KEY + bucket,
                        newRedisData.joinToString(","),
                        null,
                        true
                    )
                } else {
                    logger.info("websocket timer empty bucket $bucket, delete it")
                    redisOperation.delete(WebsocketKeys.HASH_USER_TIMEOUT_REDIS_KEY + bucket)
                }
            }
        }
        LogUtils.costTime("websocket cron", nowTime, warnThreshold = 60000, errorThreshold = 5 * 60000)
    }

    fun clearSessionByMq(userId: String, sessionId: String) {
        transferDispatch.dispatch(
            ClearSessionEvent(
                userId = userId,
                sessionId = sessionId,
                page = null,
                transferData = mutableMapOf()
            )
        )
    }
}

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

package com.tencent.devops.common.websocket.utils

import com.tencent.devops.common.redis.RedisOperation
import java.util.concurrent.TimeUnit

object WsRedisUtils {

    // 记录user-session映射。 user: session = 1:N 。同一个user，可以在不同端登录，可能产生多个session
    private const val USER_SESSION_REDIS_KEY = "WebSocket:userId:sessionId:key:"

    // 记录session-page映射。 session: page = 1:1。 同一个session一次只能停留在一个页面。
    private const val SESSION_PAGE_REDIS_KEY = "WebSocket:sessionId:page:key:"

    // 记录session-user映射。 session: user = 1:1。 同一个session一次只能对应一个user。
    private const val SESSION_USER_REDIS_KEY = "WebSocket:sessionId:user:key:"

    // 记录page-session映射。  page : session = 1:N。 同一个页面，可能有多个session停留
    private const val PAGE_SESSION_REDIS_KEY = "WebSocket:page:sessionIdList:key:"

    private val EXP_IN_30_DAYS = TimeUnit.DAYS.toSeconds(30) // 30 days expired

    // 写入user,session映射。并记录session超时时间。可能出现user:session 一对多的关系
    fun writeSessionIdByRedis(redisOperation: RedisOperation, userId: String, sessionId: String) {
        // 此处可能一个userId多端登录后有对应多个sessionId，但需要通过userId,找到对应的sessionId,故以userId为key
        val usKey = USER_SESSION_REDIS_KEY + userId
        if (redisOperation.sadd(usKey, sessionId) != 0L) {
            redisOperation.expire(usKey, expiredInSecond = EXP_IN_30_DAYS)
        }
        // 保存 sid 与 uid 的关联关系
        redisOperation.set(
            key = SESSION_USER_REDIS_KEY + sessionId,
            value = userId,
            expiredInSecond = EXP_IN_30_DAYS,
            expired = true
        )
    }

    // 获取userId对应的session集合。多个session以“,”隔开，用于做切割
    fun getSessionIdByUserId(redisOperation: RedisOperation, userId: String): Set<String>? {
        return redisOperation.getSetMembers(USER_SESSION_REDIS_KEY + userId)
    }

    // 根据sessionId刷新session-page，用于切换页面
    fun refreshSessionPage(redisOperation: RedisOperation, sessionId: String, page: String) {
        cleanSessionPageBySessionId(redisOperation, sessionId)
        redisOperation.set(SESSION_PAGE_REDIS_KEY + sessionId, page, expiredInSecond = EXP_IN_30_DAYS, expired = true)
    }

    // 刷新page-sessionIdList 数据。用于用户切换页面。
    fun refreshPageSession(redisOperation: RedisOperation, sessionId: String, newPage: String) {
        val psKey = PAGE_SESSION_REDIS_KEY + newPage
        if (redisOperation.sadd(psKey, sessionId) != 0L) {
            redisOperation.expire(psKey, expiredInSecond = EXP_IN_30_DAYS)
        }
    }

    // 根据sessionId清理sessionId-page对应的记录,sessionId：page = 1:1
    fun cleanSessionPageBySessionId(redisOperation: RedisOperation, sessionId: String) {
        redisOperation.delete(SESSION_PAGE_REDIS_KEY + sessionId)
    }

    // 根据session获取session-page记录，只能获取到一个page
    fun getPageFromSessionPageBySession(redisOperation: RedisOperation, sessionId: String): String? {
        return redisOperation.get(SESSION_PAGE_REDIS_KEY + sessionId)
    }

    // 根据session获取user记录，只能获取到一个user
    fun getUserBySession(redisOperation: RedisOperation, sessionId: String): String? {
        return redisOperation.get(SESSION_USER_REDIS_KEY + sessionId)
    }

    // 删除该session对应的User记录
    fun deleteUserSessionBySession(redisOperation: RedisOperation, sessionId: String): Boolean {
        return redisOperation.delete(SESSION_USER_REDIS_KEY + sessionId)
    }

    // 根据page获取session集合。可能有多个session
    fun getSessionListFormPageSessionByPage(redisOperation: RedisOperation, page: String): Set<String>? {
        return redisOperation.getSetMembers(PAGE_SESSION_REDIS_KEY + page)
    }

    // 清理page-sessionIdList记录。page：sessionId是一对多的关系。 切换页面或者用户登出需调用此方法
    fun cleanPageSessionBySessionId(redisOperation: RedisOperation, page: String, sessionId: String) {
        val psKey = PAGE_SESSION_REDIS_KEY + page
        if (redisOperation.sremove(psKey, sessionId) != 0L) {
            redisOperation.expire(psKey, expiredInSecond = EXP_IN_30_DAYS)
        }
    }

    // 根据page删除redis数据。build结束，需删除改次构建的所有session记录
    fun cleanPageSessionByPage(redisOperation: RedisOperation, page: String) {
        redisOperation.delete(PAGE_SESSION_REDIS_KEY + page)
    }

    // 清理user-sessionId映射内对应的sessionId, 登出需用。
    fun cleanUserSessionBySessionId(redisOperation: RedisOperation, userId: String, sessionId: String) {
        val cacheKey = USER_SESSION_REDIS_KEY + userId
        if (redisOperation.sremove(cacheKey, sessionId) != 0L) {
            redisOperation.expire(cacheKey, expiredInSecond = EXP_IN_30_DAYS)
        }
    }
}

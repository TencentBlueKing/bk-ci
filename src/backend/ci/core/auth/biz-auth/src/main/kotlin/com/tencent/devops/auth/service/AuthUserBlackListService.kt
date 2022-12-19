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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthUserBlackListDao
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisOperation
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthUserBlackListService @Autowired constructor(
    val dslContext: DSLContext,
    val authUserBlackListDao: AuthUserBlackListDao,
    val redisOperation: RedisOperation
) {

    fun createBlackListUser(
        userId: String,
        remark: String?
    ): Boolean {
        logger.info("create blackList : userId = $userId | remark = $remark")
        if (authUserBlackListDao.get(dslContext, userId) != null) {
            logger.warn("user $userId is blackList user")
            return true
        }

        redisOperation.delete(BLACKLIST_REDIS_KEY + userId)
        return authUserBlackListDao.create(
            dslContext = dslContext,
            userId = userId,
            remark = remark ?: "system create"
        ) == 1
    }

    fun removeBlackListUser(
        userId: String
    ): Boolean {
        logger.info("remove $userId blackList")
        return authUserBlackListDao.delete(dslContext, userId) == 1
    }

    fun blackList(): Set<String> {
        val blackList = mutableSetOf<String>()
        authUserBlackListDao.list(dslContext)?.map { blackList.add(it.userId) }
        return blackList
    }

    fun checkBlackListUser(
        userId: String
    ): Boolean {
        // 优先从缓存中取数据
        redisOperation.get(BLACKLIST_REDIS_KEY + userId)
        val userInfo = authUserBlackListDao.get(dslContext, userId)
        if (userInfo == null) {
            // 只缓存非名单内的用户
            redisOperation.set(BLACKLIST_REDIS_KEY + userId, "", DateTimeUtil.minuteToSecond(5).toLong())
            return false
        }
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(AuthUserBlackListService::class.java)
        const val BLACKLIST_REDIS_KEY = "bkci:auth:blackList:"
    }
}

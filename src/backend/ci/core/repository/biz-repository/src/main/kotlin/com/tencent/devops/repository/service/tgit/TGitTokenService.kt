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

package com.tencent.devops.repository.service.tgit

import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.repository.dao.TGitTokenDao
import com.tencent.devops.repository.pojo.oauth.GitToken
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TGitTokenService @Autowired constructor(
    private val dslContext: DSLContext,
    private val tGitTokenDao: TGitTokenDao,
    private val redisOperation: RedisOperation,
    private val tGitService: ITGitService
) {
    @Value("\${aes.git:#{null}}")
    private val aesKey = ""

    fun getAccessToken(userId: String): GitToken? {

        val accessToken = doGetAccessToken(userId) ?: return null

        return if (isTokenExpire(accessToken)) {
            logger.info("try to refresh the git token of user($userId)")
            val lock = RedisLock(redisOperation, "OAUTH_REFRESH_TOKEN_$userId", 60L)
            lock.use {
                lock.lock()
                val newAccessToken = doGetAccessToken(userId)!!
                if (newAccessToken.expiresIn * 1000 <= System.currentTimeMillis() - 1800 * 1000) {
                    refreshToken(userId, newAccessToken)
                } else {
                    newAccessToken
                }
            }
        } else {
            accessToken
        }
    }

    private fun doGetAccessToken(userId: String): GitToken? {
        return tGitTokenDao.getAccessToken(dslContext, userId)?.let {
            GitToken(
                accessToken = AESUtil.decrypt(aesKey, it.accessToken),
                refreshToken = AESUtil.decrypt(aesKey, it.refreshToken),
                tokenType = it.tokenType,
                expiresIn = it.expiresIn,
                createTime = it.createTime.timestampmilli(),
                oauthUserId = userId
            )
        }
    }

    private fun isTokenExpire(accessToken: GitToken): Boolean {
        // 提前半个小时刷新token
        return (accessToken.createTime ?: 0) + accessToken.expiresIn * 1000 - 1800 * 1000 <= System.currentTimeMillis()
    }

    private fun refreshToken(userId: String, gitToken: GitToken): GitToken {
        val token = tGitService.refreshToken(userId, gitToken)
        val oauthUserId = tGitService.getUserInfoByToken(token.accessToken).username ?: userId
        saveAccessToken(userId, oauthUserId, token)
        token.accessToken = AESUtil.decrypt(aesKey, token.accessToken)
        token.refreshToken = AESUtil.decrypt(aesKey, token.refreshToken)
        return token
    }

    fun saveAccessToken(userId: String, oauthUserId: String, tGitToken: GitToken): Int {
        tGitToken.accessToken = AESUtil.encrypt(aesKey, tGitToken.accessToken)
        tGitToken.refreshToken = AESUtil.encrypt(aesKey, tGitToken.refreshToken)
        return tGitTokenDao.saveAccessToken(dslContext, userId, oauthUserId, tGitToken)
    }

    fun deleteToken(userId: String): Int {
        return tGitTokenDao.deleteToken(dslContext, userId)
    }

    companion object {
        val logger = LoggerFactory.getLogger(TGitTokenService::class.java)
    }
}

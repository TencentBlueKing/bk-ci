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

package com.tencent.devops.stream.v2.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.repository.pojo.oauth.GitToken
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
@Suppress("all")
@Service
class StreamGitTokenService @Autowired constructor(
    private val streamScmService: StreamScmService,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamGitTokenService::class.java)
        private const val STREAM_GIT_TOKEN_UPDATE_LOCK_PREFIX = "stream:git:token:lock:key:"
        private const val STREAM_GIT_TOKEN_PROJECT_PREFIX = "stream:git:project:token:newKey:"
        fun getGitTokenKey(gitProjectId: Long) = STREAM_GIT_TOKEN_PROJECT_PREFIX + gitProjectId
        fun getGitTokenLockKey(gitProjectId: Long) = STREAM_GIT_TOKEN_UPDATE_LOCK_PREFIX + gitProjectId
    }

    fun getToken(gitProjectId: Long): String {
        val projectId = getGitTokenKey(gitProjectId)
        val token: GitToken? = redisOperation.get(projectId)?.let { objectMapper.readValue(it, GitToken::class.java) }
        val updateLock = RedisLock(redisOperation, getGitTokenLockKey(gitProjectId), 10)
        // 设置过期时间为24个小时
        val validTime = TimeUnit.HOURS.toSeconds(23)
        return if (token == null) {
            getToken(gitProjectId, validTime)
//            updateLock.use {
//                updateLock.lock()
//                val newToken = streamScmService.getToken(gitProjectId.toString())
//                setTokenCreateTime(newToken)
//                logger.info("STREAM|getToken|gitProjectId=$gitProjectId|newToken=${newToken.accessToken}")
//                val objJsonStr = JsonUtil.toJson(newToken, false)
//                redisOperation.set(projectId, objJsonStr, validTime)
//                newToken.accessToken
//            }
        } else {
            // 如果过期，获取使用refreshToken去刷新token
            if (isExpire(token)) {
                updateLock.use {
                    updateLock.lock()
                    try {
                        val refreshToken = streamScmService.refreshToken(gitProjectId.toString(), token.refreshToken)
                        setTokenCreateTime(refreshToken)
                        logger.info("STREAM|getToken|gitProjectId=$gitProjectId|refreshToken=${refreshToken.accessToken}")
                        val objJsonStr = JsonUtil.toJson(refreshToken, false)
                        redisOperation.set(projectId, objJsonStr, validTime)
                        refreshToken.accessToken
                    } catch (e: ErrorCodeException) {
                        logger.error("STREAM|getToken|gitProjectId=$gitProjectId|refreshTokenFail", e)
                        getToken(gitProjectId, validTime)
                    }
                }
            } else {
                // 未过期，直接返回token
                token.accessToken
            }
        }
    }

    private fun getToken(gitProjectId: Long, validTime: Long): String {
        val projectId = getGitTokenKey(gitProjectId)
        val updateLock = RedisLock(redisOperation, getGitTokenLockKey(gitProjectId), 10)
        return updateLock.use {
            updateLock.lock()
            val newToken = streamScmService.getToken(gitProjectId.toString())
            setTokenCreateTime(newToken)
            logger.info("STREAM|getToken|gitProjectId=$gitProjectId|newToken=${newToken.accessToken}")
            val objJsonStr = JsonUtil.toJson(newToken, false)
            redisOperation.set(projectId, objJsonStr, validTime)
            newToken.accessToken
        }
    }

    private fun setTokenCreateTime(newToken: GitToken) {
        newToken.createTime = System.currentTimeMillis()
    }

    private fun isExpire(accessToken: GitToken): Boolean {
        // 提前半个小时刷新token
        return (accessToken.createTime ?: 0) + accessToken.expiresIn * 1000 - 1800 * 1000 <= System.currentTimeMillis()
    }

    // TODO 暂时不加入销毁逻辑
    fun clearToken(gitProjectId: Long): Boolean {
        val token = redisOperation.get(getGitTokenKey(gitProjectId))
        if (token.isNullOrBlank()) return true
        val cleared = streamScmService.clearToken(gitProjectId, token)
        logger.info("STREAM|clearToken|gitProjectId=$gitProjectId|token=$token cleared=$cleared")
        if (cleared) {
            val updateLock = RedisLock(redisOperation, getGitTokenLockKey(gitProjectId), 10)
            updateLock.lock()
            try {
                logger.info("STREAM|deleteTokenInRedis|gitProjectId=$gitProjectId|token=$token")
                redisOperation.delete(getGitTokenKey(gitProjectId))
            } finally {
                updateLock.unlock()
            }
        }
        return cleared
    }
}

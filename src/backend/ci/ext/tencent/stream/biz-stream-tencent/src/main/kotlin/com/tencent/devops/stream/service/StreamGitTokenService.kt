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

package com.tencent.devops.stream.service

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Service
class StreamGitTokenService @Autowired constructor(
    private val streamScmService: StreamScmService,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamGitTokenService::class.java)
        private const val STREAM_GIT_TOKEN_UPDATE_LOCK_PREFIX = "stream:git:token:lock:key:"
        private const val STREAM_GIT_TOKEN_PROJECT_PREFIX = "stream:git:project:token:"
        fun getGitTokenKey(gitProjectId: Long) = STREAM_GIT_TOKEN_PROJECT_PREFIX + gitProjectId
        fun getGitTokenProjectKey(gitProjectId: String) = STREAM_GIT_TOKEN_PROJECT_PREFIX + gitProjectId
        fun getGitTokenLockKey(gitProjectId: Long) = STREAM_GIT_TOKEN_UPDATE_LOCK_PREFIX + gitProjectId

        // 工蜂超级token有效时间为8个小时，我们redis存7.5个小时，提前半个小时，这里token的使用都在初始化时间完成，不会超过30分钟
        private val validTime = TimeUnit.MINUTES.toSeconds(30) + TimeUnit.HOURS.toSeconds(7)
    }

    fun getToken(gitProjectId: Long, notGetFromCache: Boolean = false): String {
        val token = redisOperation.get(getGitTokenKey(gitProjectId))
        return if (token.isNullOrBlank() || notGetFromCache) {
            val updateLock = RedisLock(redisOperation, getGitTokenLockKey(gitProjectId), 10)
            updateLock.use {
                updateLock.lock()
                val newToken = streamScmService.getToken(gitProjectId.toString()).accessToken
                logger.info("STREAM|getToken|gitProjectId=$gitProjectId|newToken=$newToken")
                redisOperation.set(getGitTokenKey(gitProjectId), newToken, validTime)
                newToken
            }
        } else token
    }

    fun getTokenByNameWithNameSpace(gitProjectId: String, notGetFromCache: Boolean = false): String {
        val id = URLEncoder.encode(gitProjectId, "UTF8")
        val token = redisOperation.get(getGitTokenProjectKey(id))
        return if (token.isNullOrBlank() || notGetFromCache) {
            val updateLock = RedisLock(redisOperation, getGitTokenProjectKey(id), 10)
            updateLock.use {
                updateLock.lock()
                val newToken = streamScmService.getTokenForProject(id)!!.accessToken
                logger.info("STREAM|getToken|gitProjectId=$id|newToken=$newToken")
                redisOperation.set(getGitTokenProjectKey(id), newToken, validTime)
                newToken
            }
        } else token
    }

    // TODO 暂时不加入销毁逻辑
    fun clearToken(gitProjectId: Long): Boolean {
        val token = redisOperation.get(getGitTokenKey(gitProjectId))
        if (token.isNullOrBlank()) return true
        val cleared = streamScmService.clearToken(gitProjectId, token)
        logger.info("STREAM|clearToken|gitProjectId=$gitProjectId|token=$token cleared=$cleared")
        if (cleared) {
            val updateLock = RedisLock(redisOperation, getGitTokenLockKey(gitProjectId), 10)
            updateLock.use {
                updateLock.lock()
                logger.info("STREAM|deleteTokenInRedis|gitProjectId=$gitProjectId|token=$token")
                redisOperation.delete(getGitTokenKey(gitProjectId))
            }
        }
        return cleared
    }
}

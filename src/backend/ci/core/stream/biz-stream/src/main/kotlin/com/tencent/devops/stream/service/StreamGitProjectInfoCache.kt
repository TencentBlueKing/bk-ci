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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.stream.pojo.StreamGitProjectBaseInfoCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StreamGitProjectInfoCache @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation,
    private val streamGitTransferService: StreamGitTransferService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamGitProjectBaseInfoCache::class.java)

        //  过期时间目前设为1小时
        private const val STREAM_CACHE_EXPIRE_TIME = 60 * 60L
        private const val STREAM_GIT_PROJECT_KEY_PREFIX = "stream:gitProjectInfo"
    }

    fun getAndSaveGitProjectInfo(
        gitProjectId: Long,
        useAccessToken: Boolean,
        userId: String?,
        accessToken: String? = null
    ): StreamGitProjectBaseInfoCache? {
        val cache = getRequestGitProjectInfo(gitProjectName = gitProjectId.toString())
        if (cache != null) {
            return cache
        }

        val cacheData = try {
            streamGitTransferService.getGitProjectCache(
                gitProjectId = gitProjectId.toString(),
                useAccessToken = useAccessToken,
                userId = userId,
                accessToken = accessToken
            )
        } catch (ignored: Throwable) {
            logger.warn(
                "StreamGitProjectInfoCache|getAndSaveGitProjectInfo" +
                    "|update cache error",
                ignored
            )
            return null
        }

        saveRequestGitProjectInfo(
            gitProjectName = gitProjectId.toString(),
            cache = cacheData
        )
        return cacheData
    }

    /**
     * 保存stream 项目基本不变信息，降低网络IO时延
     */
    fun saveRequestGitProjectInfo(
        gitProjectName: String,
        cache: StreamGitProjectBaseInfoCache
    ) {
        redisOperation.set(
            key = "$STREAM_GIT_PROJECT_KEY_PREFIX:$gitProjectName",
            value = JsonUtil.toJson(cache),
            expiredInSecond = STREAM_CACHE_EXPIRE_TIME
        )
    }

    fun getRequestGitProjectInfo(gitProjectName: String): StreamGitProjectBaseInfoCache? {
        return try {
            val result = redisOperation.get(
                "$STREAM_GIT_PROJECT_KEY_PREFIX:$gitProjectName"
            )
            if (result != null) {
                objectMapper.readValue<StreamGitProjectBaseInfoCache>(result)
            } else {
                null
            }
        } catch (ignore: Exception) {
            logger.warn(
                "StreamGitProjectInfoCache|getRequestGitProjectInfo" +
                    "$gitProjectName|error",
                ignore
            )
            null
        }
    }
}

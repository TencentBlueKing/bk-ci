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

package com.tencent.devops.stream.v1.components

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.stream.service.StreamGitTokenService
import com.tencent.devops.stream.v1.pojo.V1GitProjectCache
import com.tencent.devops.stream.v1.pojo.V1StreamGitProjectCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class V1StreamGitProjectInfoCache @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation,
    private val streamGitTokenService: StreamGitTokenService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(V1StreamGitProjectCache::class.java)

        //  过期时间目前设为1小时
        private const val STREAM_CACHE_EXPIRE_TIME = 60 * 60L
        private const val STREAM_GIT_PROJECT_KEY_PREFIX = "stream:gitProjectInfo"
    }

    fun getAndSaveGitProjectInfo(
        gitProjectId: Long,
        useAccessToken: Boolean,
        getProjectInfo: (
            token: String,
            gitProjectId: String,
            useAccessToken: Boolean
        ) -> GitCIProjectInfo
    ): V1GitProjectCache? {
        val cache = getRequestGitProjectInfo(gitProjectName = gitProjectId.toString())
        if (cache != null) {
            return cache
        }
        val gitProjectInfo = try {
            getProjectInfo(
                streamGitTokenService.getToken(gitProjectId),
                gitProjectId.toString(),
                useAccessToken
            )
        } catch (ignored: Throwable) {
            logger.warn("fail to get prject token ,may be it is delete ${ignored.message}", ignored)
            return null
        }
        val cacheData = V1GitProjectCache(
            gitProjectId = gitProjectInfo.gitProjectId,
            gitHttpUrl = gitProjectInfo.gitHttpUrl,
            homepage = gitProjectInfo.homepage,
            pathWithNamespace = gitProjectInfo.pathWithNamespace
        )
        saveRequestGitProjectInfo(
            gitProjectName = gitProjectId.toString(),
            cache = cacheData
        )
        return cacheData
    }

    /**
     * 保存工蜂项目基本不变信息，降低网络IO时延
     */
    fun saveRequestGitProjectInfo(
        gitProjectName: String,
        cache: V1GitProjectCache
    ) {
        redisOperation.set(
            key = "$STREAM_GIT_PROJECT_KEY_PREFIX:$gitProjectName",
            value = JsonUtil.toJson(cache),
            expiredInSecond = STREAM_CACHE_EXPIRE_TIME
        )
    }

    fun getRequestGitProjectInfo(gitProjectName: String): V1GitProjectCache? {
        return try {
            val result = redisOperation.get(
                "$STREAM_GIT_PROJECT_KEY_PREFIX:$gitProjectName"
            )
            if (result != null) {
                objectMapper.readValue<V1GitProjectCache>(result)
            } else {
                null
            }
        } catch (ignore: Exception) {
            logger.warn("stream request gitProjectInfo cache get$gitProjectName error", ignore)
            null
        }
    }
}

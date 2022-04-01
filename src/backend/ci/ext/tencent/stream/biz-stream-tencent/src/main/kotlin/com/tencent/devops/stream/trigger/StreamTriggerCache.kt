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

package com.tencent.devops.stream.trigger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.stream.trigger.pojo.StreamGitProjectCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StreamTriggerCache @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamTriggerCache::class.java)

        //  过期时间目前设为1小时
        private const val STREAM_CACHE_EXPIRE_TIME = 10 * 60L
        private const val STREAM_REQUEST_KEY_PREFIX = "stream:request"
    }

    fun getAndSaveRequestGitProjectInfo(
        gitRequestEventId: Long,
        gitProjectId: String,
        token: String,
        useAccessToken: Boolean,
        getProjectInfo: (
            token: String,
            gitProjectId: String,
            useAccessToken: Boolean
        ) -> GitCIProjectInfo
    ): StreamGitProjectCache {
        val cache = getRequestGitProjectInfo(gitRequestEventId = gitRequestEventId, gitProjectName = gitProjectId)
        if (cache != null) {
            return cache
        }
        val gitProjectInfo = getProjectInfo(
            token,
            gitProjectId,
            useAccessToken
        )
        val cacheData = StreamGitProjectCache(
            gitProjectName = gitProjectId,
            gitProjectId = gitProjectInfo.gitProjectId,
            defaultBranch = gitProjectInfo.defaultBranch,
            gitHttpUrl = gitProjectInfo.gitHttpUrl,
            name = gitProjectInfo.name
        )
        saveRequestGitProjectInfo(
            gitRequestEventId = gitRequestEventId,
            gitProjectName = gitProjectId,
            cache = cacheData
        )
        return cacheData
    }

    /**
     * 保存某一次request触发时获取的各个工蜂项目名称和信息的KEY-VALUE，便于多条流水线时直接拿取降低网络IO
     * projectName: 可以是 gitProjectId.toString() 也可为 gitProject.pathWithPathSpace
     */
    fun saveRequestGitProjectInfo(
        gitRequestEventId: Long,
        gitProjectName: String,
        cache: StreamGitProjectCache
    ) {
        redisOperation.set(
            key = "$STREAM_REQUEST_KEY_PREFIX:$gitRequestEventId:gitProjectInfo:$gitProjectName",
            value = JsonUtil.toJson(cache),
            expiredInSecond = STREAM_CACHE_EXPIRE_TIME
        )
    }

    fun getRequestGitProjectInfo(gitRequestEventId: Long, gitProjectName: String): StreamGitProjectCache? {
        return try {
            val result = redisOperation.get(
                "$STREAM_REQUEST_KEY_PREFIX:$gitRequestEventId:gitProjectInfo:$gitProjectName"
            )
            if (result != null) {
                objectMapper.readValue<StreamGitProjectCache>(result)
            } else {
                null
            }
        } catch (ignore: Exception) {
            logger.warn("stream request gitProjectInfo cache get $gitRequestEventId|$gitProjectName error", ignore)
            null
        }
    }
}

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

package com.tencent.devops.stream.trigger.parsers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.StreamGitProjectInfo
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

    /**
     * 保存某一次request触发时获取的各个stream 项目名称和信息的KEY-VALUE，便于多条流水线时直接拿取降低网络IO
     * @param gitProjectKey: 可以是 gitProjectId.toString() 也可为 gitProject.pathWithPathSpace
     */
    fun getAndSaveRequestGitProjectInfo(
        gitProjectKey: String,
        action: BaseAction,
        getProjectInfo: (
            cred: StreamGitCred,
            gitProjectId: String,
            retry: ApiRequestRetryInfo
        ) -> StreamGitProjectInfo?,
        cred: StreamGitCred? = null
    ): StreamGitProjectCache? {
        val cache = getRequestGitProjectInfo(
            gitRequestEventId = action.data.context.requestEventId!!,
            gitProjectKey = gitProjectKey
        )
        if (cache != null) {
            return cache
        }
        val gitProjectInfo = try {
            getProjectInfo(
                // 优先级： 传入凭据 -> 远程触发凭据 -> ci开启人
                cred ?: action.data.context.repoTrigger?.repoTriggerCred ?: action.getGitCred(),
                action.getGitProjectIdOrName(gitProjectKey),
                ApiRequestRetryInfo(true)
            ) ?: return null
        } catch (e: ErrorCodeException) {
            logger.warn(
                "Get project info error|$gitProjectKey|${cred != null}|" +
                    "${action.data.context.repoTrigger?.buildUserID}|${action.data.setting.enableUser}",
                e
            )
            return null
        }
        val cacheData = StreamGitProjectCache(
            gitProjectId = gitProjectInfo.gitProjectId,
            defaultBranch = gitProjectInfo.defaultBranch,
            gitHttpUrl = gitProjectInfo.gitHttpUrl,
            name = gitProjectInfo.name,
            gitSshUrl = gitProjectInfo.gitSshUrl,
            homepage = gitProjectInfo.homepage,
            gitHttpsUrl = gitProjectInfo.gitHttpsUrl,
            description = gitProjectInfo.description,
            avatarUrl = gitProjectInfo.avatarUrl,
            pathWithNamespace = gitProjectInfo.pathWithNamespace,
            nameWithNamespace = gitProjectInfo.nameWithNamespace,
            repoCreatedTime = gitProjectInfo.repoCreatedTime,
            repoCreatorId = gitProjectInfo.repoCreatorId
        )

        saveRequestGitProjectInfo(
            gitRequestEventId = action.data.context.requestEventId!!,
            gitProjectKey = gitProjectKey,
            cache = cacheData
        )
        return cacheData
    }

    /**
     * 保存某一次request触发时获取的各个stream 项目名称和信息的KEY-VALUE，便于多条流水线时直接拿取降低网络IO
     */
    fun saveRequestGitProjectInfo(
        gitRequestEventId: Long,
        gitProjectKey: String,
        cache: StreamGitProjectCache
    ) {
        redisOperation.set(
            key = "$STREAM_REQUEST_KEY_PREFIX:$gitRequestEventId:gitProjectInfo:$gitProjectKey",
            value = JsonUtil.toJson(cache),
            expiredInSecond = STREAM_CACHE_EXPIRE_TIME
        )
    }

    fun getRequestGitProjectInfo(gitRequestEventId: Long, gitProjectKey: String): StreamGitProjectCache? {
        return try {
            val result = redisOperation.get(
                "$STREAM_REQUEST_KEY_PREFIX:$gitRequestEventId:gitProjectInfo:$gitProjectKey"
            )
            if (result != null) {
                objectMapper.readValue<StreamGitProjectCache>(result)
            } else {
                null
            }
        } catch (ignore: Exception) {
            logger.warn(
                "StreamTriggerCache|getRequestGitProjectInfo|error" +
                    "|eventId|$gitRequestEventId|projectKey|$gitProjectKey",
                ignore
            )
            null
        }
    }
}

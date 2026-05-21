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

package com.tencent.devops.scm.services

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.scm.pojo.tapd.TapdBug
import com.tencent.devops.scm.pojo.tapd.TapdStory
import com.tencent.devops.scm.config.TapdProperties
import com.tencent.devops.scm.pojo.tapd.BugResponse
import com.tencent.devops.scm.pojo.tapd.StoryResponse
import com.tencent.devops.scm.pojo.tapd.TapdResult
import com.tencent.devops.scm.utils.RetryUtils
import okhttp3.Credentials
import okhttp3.Headers
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * TAPD 业务对象查询服务实现
 */
@Service
class TapdItemService @Autowired constructor(
    val tapdConfig: TapdProperties
) {

    fun getStoryInfo(
        workspaceId: String,
        storyId: String
    ): TapdStory? {
        if (workspaceId.isBlank() || storyId.isBlank()) {
            logger.warn("invalid tapd story query|workspaceId=$workspaceId|storyId=$storyId")
            return null
        }
        val url = "${tapdConfig.apiUrl.removeSuffix("/")}/stories".addParams(
            mapOf(
                "workspace_id" to workspaceId,
                "id" to storyId
            )
        )
        val request = Request.Builder()
                .url(url)
                .headers(authHeaders())
                .get()
                .build()
        RetryUtils.doRetryHttp(request).use { response ->
            if (!response.isSuccessful) {
                throw RemoteServiceException(
                    httpStatus = response.code,
                    errorMessage = "(${response.code})${response.message}"
                )
            }
            return response.body?.string()?.takeIf { it.isNotBlank() }?.let {
                JsonUtil.toOrNull(
                    it,
                    object : TypeReference<TapdResult<List<StoryResponse>>>() {}
                )?.data?.firstOrNull()?.story
            }
        }
    }

    fun getBugInfo(
        workspaceId: String,
        bugId: String
    ): TapdBug? {
        if (workspaceId.isBlank() || bugId.isBlank()) {
            logger.warn("invalid tapd bug query|workspaceId=$workspaceId|bugId=$bugId")
            return null
        }
        val url = "${tapdConfig.apiUrl.removeSuffix("/")}/bugs".addParams(
            mapOf(
                "workspace_id" to workspaceId,
                "id" to bugId
            )
        )
        val request = Request.Builder()
                .url(url)
                .headers(authHeaders())
                .get()
                .build()
        RetryUtils.doRetryHttp(request).use { response ->
            if (!response.isSuccessful) {
                throw RemoteServiceException(
                    httpStatus = response.code,
                    errorMessage = "(${response.code})${response.message}"
                )
            }
            return response.body?.string()?.takeIf { it.isNotBlank() }?.let {
                JsonUtil.toOrNull(
                    it,
                    object : TypeReference<TapdResult<List<BugResponse>>>() {}
                )?.data?.firstOrNull()?.bug
            }
        }
    }

    private fun String.addParams(args: Map<String, Any?>): String {
        val sb = StringBuilder(this)
        args.forEach { (name, value) ->
            if (value != null) {
                sb.append("&$name=$value")
            }
        }
        return sb.toString()
    }

    private fun authHeaders() = Headers.Builder().add(
        "Authorization", Credentials.basic(tapdConfig.clientId, tapdConfig.clientSecret)
    ).build()

    companion object {
        private val logger = LoggerFactory.getLogger(TapdItemService::class.java)
    }
}

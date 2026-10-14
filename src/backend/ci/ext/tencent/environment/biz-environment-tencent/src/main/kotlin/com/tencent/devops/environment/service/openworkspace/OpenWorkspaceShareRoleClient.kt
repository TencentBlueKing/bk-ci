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

package com.tencent.devops.environment.service.openworkspace

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.pojo.workspace.WorkspaceShareRoleQueryItem
import com.tencent.devops.environment.pojo.workspace.WorkspaceShareRoleResult
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class OpenWorkspaceShareRoleClient(
    @Value("\${iegImate.openWorkspace.baseUrl:}")
    private val baseUrl: String,
    @Value("\${iegImate.openWorkspace.appId:}")
    private val appId: String,
    @Value("\${iegImate.openWorkspace.secret:}")
    private val secret: String
) {
    /**
     * 批量查询用户对团队共享分身的有效角色。
     * 入参为空时直接返回空列表；鉴权失败 / 网络异常返回 null，由调用方按失败关闭处理。
     */
    fun queryShareAccessRoles(
        items: List<WorkspaceShareRoleQueryItem>
    ): List<WorkspaceShareRoleResult>? {
        if (items.isEmpty()) {
            return emptyList()
        }
        if (baseUrl.isBlank() || appId.isBlank()) {
            logger.warn(
                "Skip query share access roles|baseUrlBlank={}|appIdBlank={}",
                baseUrl.isBlank(),
                appId.isBlank()
            )
            return null
        }
        logger.info("Query share access roles|size={}", items.size)
        return try {
            val request = Request.Builder()
                .url(buildRequestUrl())
                .header(APP_ID_HEADER, appId)
                .header(SECRET_HEADER, secret)
                .post(JsonUtil.toJson(items, false).toRequestBody(JSON_MEDIA_TYPE))
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    logger.warn(
                        "Fail to query share access roles|code={}|message={}|response={}",
                        response.code,
                        response.message,
                        responseContent
                    )
                    return null
                }
                parseResponse(responseContent)
            }
        } catch (ignored: Exception) {
            logger.warn("Fail to query share access roles|size={}|message={}", items.size, ignored.message, ignored)
            null
        }
    }

    internal fun parseResponse(responseContent: String): List<WorkspaceShareRoleResult>? {
        val result = JsonUtil.to(
            responseContent,
            object : TypeReference<Result<List<WorkspaceShareRoleResult>>>() {}
        )
        if (result.isNotOk()) {
            logger.warn(
                "Share access role api returned error|status={}|message={}",
                result.status,
                result.message
            )
            return null
        }
        val roles = result.data.orEmpty()
        logger.info("Query share access roles success|resultSize={}|roles={}", roles.size, roles)
        return roles
    }

    private fun buildRequestUrl(): String {
        val builder = baseUrl.removeSuffix("/").toHttpUrl().newBuilder()
        QUERY_PATH.trimStart('/').split("/").filter { it.isNotBlank() }.forEach { segment ->
            builder.addPathSegment(segment)
        }
        return builder.build().toString()
    }

    companion object {
        private const val APP_ID_HEADER = "x-app-id"
        private const val SECRET_HEADER = "x-secret"
        private const val QUERY_PATH = "/ai/api/open/workspace/share-access-roles/query"
        private val JSON_MEDIA_TYPE = "application/json".toMediaTypeOrNull()
        private val logger = LoggerFactory.getLogger(OpenWorkspaceShareRoleClient::class.java)
    }
}

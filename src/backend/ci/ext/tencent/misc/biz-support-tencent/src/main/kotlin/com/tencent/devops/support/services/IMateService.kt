/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝盾持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝盾持续集成平台 is licensed under the MIT license.
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

package com.tencent.devops.support.services

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.support.model.imate.IMateAuthorizationInfo
import com.tencent.devops.support.model.imate.IMateClientType
import com.tencent.devops.support.model.imate.IMateRobotInfo
import com.tencent.devops.support.model.imate.IMateRobotOwnerType
import com.tencent.devops.support.model.imate.IMateVisibleTargetInfo
import com.tencent.devops.support.model.imate.IMateVisibleTargetType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class IMateService {

    @Value("\${imate.base-url}")
    private lateinit var baseUrl: String

    @Value("\${imate.token}")
    private lateinit var token: String

    fun queryUserRobots(username: String): Result<List<IMateRobotInfo>> {
        val result = request(
            path = QUERY_USER_ROBOT_PATH,
            queryParams = mapOf("username" to username),
            typeReference = object : TypeReference<IMateOpenApiResponse<List<IMateRobotResponse>>>() {}
        )
        if (result.isNotOk()) {
            return Result(status = result.status, message = result.message)
        }
        return Result(
            result.data.orEmpty().map { robot ->
                toRobotInfo(
                    username = username,
                    robot = robot
                )
            }
        )
    }

    fun getVisibleTargets(username: String, clientUuid: String): Result<List<IMateVisibleTargetInfo>> {
        val result = request(
            path = GET_VISIBLE_TARGETS_PATH,
            queryParams = mapOf(
                "username" to username,
                "clientUuid" to clientUuid
            ),
            typeReference = object : TypeReference<IMateOpenApiResponse<IMateVisibleTargetsData>>() {}
        )
        if (result.isNotOk()) {
            return Result(status = result.status, message = result.message)
        }
        return Result(
            result.data?.visibleTargets.orEmpty().map { target ->
                IMateVisibleTargetInfo(
                    targetType = IMateVisibleTargetType.fromValue(target.targetType),
                    targetId = target.targetId.orEmpty(),
                    targetName = target.targetName.orEmpty()
                )
            }
        )
    }

    fun checkAuthorization(username: String, clientUuid: String): Result<IMateAuthorizationInfo> {
        val result = request(
            path = CHECK_AUTHORIZATION_PATH,
            queryParams = mapOf(
                "username" to username,
                "clientUuid" to clientUuid
            ),
            typeReference = object : TypeReference<IMateOpenApiResponse<IMateAuthorizationData>>() {}
        )
        if (result.isNotOk()) {
            return Result(status = result.status, message = result.message)
        }
        val data = result.data ?: IMateAuthorizationData()
        return Result(
            IMateAuthorizationInfo(
                authorized = data.isAuthorized,
                tokenAuthorized = data.isTokenAuthorized,
                canChat = data.isAuthorized && data.isTokenAuthorized,
                authorizationUrl = buildAuthorizationUrl(clientUuid)
            )
        )
    }

    internal fun buildAuthorizationUrl(clientUuid: String): String {
        return baseUrl.removeSuffix("/").toHttpUrl().newBuilder()
            .encodedPath("/oauth")
            .setQueryParameter("deviceId", clientUuid)
            .build()
            .toString()
    }

    internal fun toRobotInfo(username: String, robot: IMateRobotResponse): IMateRobotInfo {
        val clientType = IMateClientType.fromValue(robot.clientType)
        return IMateRobotInfo(
            id = robot.id,
            botName = robot.botName.orEmpty(),
            username = robot.username.orEmpty(),
            clientUuid = robot.clientUuid.orEmpty(),
            clientType = robot.clientType,
            robotScopeType = clientType.robotScopeType,
            ownerType = if (robot.username == username) {
                IMateRobotOwnerType.SELF_CREATED
            } else {
                IMateRobotOwnerType.SHARED_TO_USER
            },
            status = robot.status,
            url = robot.url,
            authorizationUrl = buildAuthorizationUrl(robot.clientUuid.orEmpty()),
            createdAt = robot.createdAt,
            updatedAt = robot.updatedAt
        )
    }

    private fun <T> request(
        path: String,
        queryParams: Map<String, String>,
        typeReference: TypeReference<IMateOpenApiResponse<T>>
    ): Result<T?> {
        return try {
            val request = Request.Builder()
                .url(buildRequestUrl(path, queryParams))
                .header(TOKEN_HEADER, token)
                .get()
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    logger.warn(
                        "Fail to request imate api|path={}|code={}|message={}|response={}",
                        path,
                        response.code,
                        response.message,
                        responseContent
                    )
                    return Result(response.code, response.message.ifBlank { "imate request failed" })
                }
                val responseObject = JsonUtil.to(responseContent, typeReference)
                if (responseObject.code != 0) {
                    logger.warn(
                        "imate api returned error|path={}|code={}|message={}",
                        path,
                        responseObject.code,
                        responseObject.message
                    )
                    return Result(responseObject.code, responseObject.message ?: "imate request failed")
                }
                Result(responseObject.data)
            }
        } catch (ignored: Exception) {
            logger.warn(
                "Fail to request imate api|path={}|queryParams={}|message={}",
                path,
                queryParams,
                ignored.message,
                ignored
            )
            Result(SYSTEM_ERROR_STATUS, ignored.message ?: "imate request failed")
        }
    }

    private fun buildRequestUrl(path: String, queryParams: Map<String, String>): String {
        val builder = baseUrl.removeSuffix("/").toHttpUrl().newBuilder()
        path.trimStart('/').split("/").filter { it.isNotBlank() }.forEach { segment ->
            builder.addPathSegment(segment)
        }
        queryParams.forEach { (key, value) ->
            builder.addQueryParameter(key, value)
        }
        return builder.build().toString()
    }

    private data class IMateOpenApiResponse<T>(
        val code: Int,
        val message: String? = null,
        val data: T? = null
    )

    internal data class IMateRobotResponse(
        val id: Long? = null,
        val botName: String? = null,
        val username: String? = null,
        val clientUuid: String? = null,
        val clientType: String? = null,
        val status: String? = null,
        val url: String? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null
    )

    private data class IMateVisibleTargetsData(
        val visibleTargets: List<IMateVisibleTargetResponse>? = null
    )

    private data class IMateVisibleTargetResponse(
        val targetType: String? = null,
        val targetId: String? = null,
        val targetName: String? = null
    )

    private data class IMateAuthorizationData(
        val isAuthorized: Boolean = false,
        val isTokenAuthorized: Boolean = false
    )

    companion object {
        private const val TOKEN_HEADER = "Token"
        private const val SYSTEM_ERROR_STATUS = 500
        private const val QUERY_USER_ROBOT_PATH = "/server/web-api/openapi/v1/queryUserRobot"
        private const val GET_VISIBLE_TARGETS_PATH = "/server/web-api/openapi/v1/getVisibleTargets"
        private const val CHECK_AUTHORIZATION_PATH = "/server/web-api/openapi/v1/checkAuthorization"
        private val logger = LoggerFactory.getLogger(IMateService::class.java)
    }
}

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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.support.model.imate.IMateAuthorizationInfo
import com.tencent.devops.support.model.imate.IMateClientType
import com.tencent.devops.support.model.imate.IMateRobotGrayControlInfo
import com.tencent.devops.support.model.imate.IMateRobotInfo
import com.tencent.devops.support.model.imate.IMateRobotOwnerType
import com.tencent.devops.support.model.imate.IMateRobotWorkspaceInfo
import com.tencent.devops.support.model.imate.IMateTaskResp
import com.tencent.devops.support.model.imate.IMateVisibleTargetInfo
import com.tencent.devops.support.model.imate.IMateVisibleTargetType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
                authorized = data.authorized,
                tokenAuthorized = data.tokenAuthorized,
                canChat = data.authorized && data.tokenAuthorized,
                authorizationUrl = buildAuthorizationUrl(clientUuid)
            )
        )
    }

    fun installLandunPlugin(
        username: String,
        clientUuid: String,
        token: String
    ): Result<IMateTaskResp?> {
        val result = request(
            path = INSTALL_LANDUN_PLUGIN_PATH,
            queryParams = emptyMap(),
            body = JsonUtil.toJson(
                mapOf(
                    "username" to username,
                    "clientUuid" to clientUuid,
                    "token" to token
                ), false
            ),
            typeReference = object : TypeReference<IMateOpenApiResponse<IMateTaskResp>>() {}
        )
        if (result.isNotOk()) {
            return Result(status = result.status, message = result.message)
        }
        return Result(
            result.data?.let { IMateTaskResp(it.taskId) }
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
        val clientUuid = robot.clientUuid.orEmpty()
        return IMateRobotInfo(
            id = robot.id,
            botName = robot.botName.orEmpty(),
            username = robot.username.orEmpty(),
            clientUuid = clientUuid,
            clientAliasName = robot.clientAliasName,
            clientDescription = robot.clientDescription,
            clientIp = robot.clientIp,
            workspaceId = robot.workspaceId,
            model = robot.model,
            gitRepos = robot.gitRepos,
            gatewayUrl = robot.gatewayUrl,
            status = robot.status,
            webhookUrl = robot.webhookUrl,
            token = robot.token,
            encodingAesKey = robot.encodingAesKey,
            createdAt = robot.createdAt,
            updatedAt = robot.updatedAt,
            url = robot.url,
            source = robot.source,
            ideUrl = robot.ideUrl,
            master = robot.master,
            clientType = robot.clientType,
            envId = robot.envId,
            openEnvUrl = robot.openEnvUrl,
            sandboxId = robot.sandboxId,
            cvdEnvId = robot.cvdEnvId,
            itfsSpaceId = robot.itfsSpaceId,
            workspace = robot.workspace?.toInfo(),
            statusModifyAt = robot.statusModifyAt,
            websocketBotId = robot.websocketBotId,
            websocketBotSecret = robot.websocketBotSecret,
            connectType = robot.connectType,
            openclawVersion = robot.openclawVersion,
            deviceId = robot.deviceId,
            deviceName = robot.deviceName,
            wxBotToken = robot.wxBotToken,
            wxLinkBotId = robot.wxLinkBotId,
            wxLinkUserId = robot.wxLinkUserId,
            wxBaseUrl = robot.wxBaseUrl,
            wxbotStatus = robot.wxbotStatus,
            websocketBotStatus = robot.websocketBotStatus,
            grayControl = robot.grayControl?.toInfo(),
            coverKey = robot.coverKey,
            forceUpdate = robot.forceUpdate,
            yuanbaoAppKey = robot.yuanbaoAppKey,
            yuanbaoAppSecret = robot.yuanbaoAppSecret,
            yuanbaoBotStatus = robot.yuanbaoBotStatus,
            taihuClientId = robot.taihuClientId,
            robotScopeType = clientType.robotScopeType,
            ownerType = if (robot.username == username) {
                IMateRobotOwnerType.SELF_CREATED
            } else {
                IMateRobotOwnerType.SHARED_TO_USER
            },
            authorizationUrl = buildAuthorizationUrl(clientUuid)
        )
    }

    private fun <T> request(
        path: String,
        queryParams: Map<String, String>,
        typeReference: TypeReference<IMateOpenApiResponse<T>>,
        body: String? = null,
    ): Result<T?> {
        return try {
            val request = if (body == null) {
                Request.Builder()
                    .url(buildRequestUrl(path, queryParams))
                    .header(TOKEN_HEADER, token)
                    .get()
                    .build()
            } else {
                Request.Builder()
                    .url(buildRequestUrl(path, queryParams))
                    .header(TOKEN_HEADER, token)
                    .post(body.toRequestBody(MediaTypes.APPLICATION_JSON.toMediaTypeOrNull()))
                    .build()
            }
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
        val clientAliasName: String? = null,
        val clientDescription: String? = null,
        val clientIp: String? = null,
        val workspaceId: String? = null,
        val model: String? = null,
        val gitRepos: List<Any>? = null,
        val gatewayUrl: String? = null,
        val status: String? = null,
        val webhookUrl: String? = null,
        val token: String? = null,
        val encodingAesKey: String? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
        val url: String? = null,
        val source: String? = null,
        val ideUrl: String? = null,
        @JsonProperty("isMaster")
        val master: Int? = null,
        val clientType: String? = null,
        val envId: String? = null,
        val openEnvUrl: String? = null,
        val sandboxId: String? = null,
        val cvdEnvId: String? = null,
        val itfsSpaceId: String? = null,
        val workspace: IMateRobotWorkspaceResponse? = null,
        val statusModifyAt: String? = null,
        val websocketBotId: String? = null,
        val websocketBotSecret: String? = null,
        val connectType: String? = null,
        val openclawVersion: String? = null,
        val deviceId: String? = null,
        val deviceName: String? = null,
        val wxBotToken: String? = null,
        val wxLinkBotId: String? = null,
        val wxLinkUserId: String? = null,
        val wxBaseUrl: String? = null,
        val wxbotStatus: String? = null,
        val websocketBotStatus: String? = null,
        val grayControl: IMateRobotGrayControlResponse? = null,
        val coverKey: String? = null,
        @JsonProperty("isForceUpdate")
        val forceUpdate: Boolean? = null,
        val yuanbaoAppKey: String? = null,
        val yuanbaoAppSecret: String? = null,
        @JsonProperty("yuanbaoBotStaus")
        val yuanbaoBotStatus: String? = null,
        val taihuClientId: String? = null
    )

    internal data class IMateRobotWorkspaceResponse(
        val id: String? = null,
        val name: String? = null,
        val userId: String? = null,
        val username: String? = null,
        val type: String? = null,
        val environmentId: String? = null,
        val status: String? = null,
        val deletedAt: String? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
        val attributes: Map<String, Any>? = null
    ) {
        fun toInfo(): IMateRobotWorkspaceInfo = IMateRobotWorkspaceInfo(
            id = id,
            name = name,
            userId = userId,
            username = username,
            type = type,
            environmentId = environmentId,
            status = status,
            deletedAt = deletedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            attributes = attributes
        )
    }

    internal data class IMateRobotGrayControlResponse(
        @JsonProperty("isSafeModelControl")
        val safeModelControl: Boolean? = null,
        @JsonProperty("isSafeEnvironmentControl")
        val safeEnvironmentControl: Boolean? = null
    ) {
        fun toInfo(): IMateRobotGrayControlInfo = IMateRobotGrayControlInfo(
            safeModelControl = safeModelControl,
            safeEnvironmentControl = safeEnvironmentControl
        )
    }

    private data class IMateVisibleTargetsData(
        val visibleTargets: List<IMateVisibleTargetResponse>? = null
    )

    private data class IMateVisibleTargetResponse(
        val targetType: String? = null,
        val targetId: String? = null,
        val targetName: String? = null
    )

    private data class IMateAuthorizationData(
        @JsonProperty("isAuthorized")
        val authorized: Boolean = false,
        @JsonProperty("isTokenAuthorized")
        val tokenAuthorized: Boolean = false
    )

    companion object {
        private const val TOKEN_HEADER = "Token"
        private const val SYSTEM_ERROR_STATUS = 500
        private const val QUERY_USER_ROBOT_PATH = "/server/web-api/openapi/v1/queryUserRobot"
        private const val GET_VISIBLE_TARGETS_PATH = "/server/web-api/openapi/v1/getVisibleTargets"
        private const val CHECK_AUTHORIZATION_PATH = "/server/web-api/openapi/v1/checkAuthorization"
        private const val INSTALL_LANDUN_PLUGIN_PATH = "/server/web-api/openapi/v1/installLandunPlugin"
        private val logger = LoggerFactory.getLogger(IMateService::class.java)
    }
}

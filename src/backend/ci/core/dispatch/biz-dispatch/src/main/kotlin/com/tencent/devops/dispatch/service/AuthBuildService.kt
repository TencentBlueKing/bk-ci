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

package com.tencent.devops.dispatch.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.dispatch.sdk.service.DispatchService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.AuthBuildInfo
import com.tencent.devops.dispatch.pojo.AuthBuildResponse
import com.tencent.devops.dispatch.utils.redis.ThirdPartyAgentBuildRedisUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthBuildService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val thirdPartyAgentBuildRedisUtils: ThirdPartyAgentBuildRedisUtils,
    private val dispatchService: DispatchService
) {

    @Value("\${dispatch.external.auth.token:}")
    private lateinit var externalAuthToken: String

    companion object {
        private val logger = LoggerFactory.getLogger(AuthBuildService::class.java)
    }

    /**
     * 第三方构建机鉴权
     */
    fun authAgent(
        secretKey: String,
        agentId: String,
        buildId: String,
        vmSeqId: String?,
        token: String
    ): AuthBuildResponse {
        return performAuth(
            token = token,
            authType = "agent",
            cacheKeyProvider = {
                thirdPartyAgentBuildRedisUtils.thirdPartyBuildKey(secretKey, agentId, buildId, vmSeqId ?: "")
            },
            requiredFields = listOf("projectId", "pipelineId", "buildId", "vmSeqId", "agentId"),
            additionalValidation = { authInfo ->
                if (authInfo.agentId != agentId) {
                    logger.warn("AgentId not match for agent auth")
                    createErrorResponse("AgentId not match")
                } else null
            },
            responseBuilder = { authInfo, _ ->
                AuthBuildResponse(
                    success = true,
                    projectId = authInfo.projectId,
                    pipelineId = authInfo.pipelineId,
                    buildId = authInfo.buildId,
                    agentId = authInfo.agentId,
                    vmSeqId = authInfo.vmSeqId,
                    vmName = authInfo.vmName,
                    channelCode = authInfo.channelCode,
                    secretKey = secretKey
                )
            }
        )
    }

    /**
     * Docker构建机鉴权
     */
    fun authDocker(
        secretKey: String,
        agentId: String,
        token: String
    ): AuthBuildResponse {
        return performAuth(
            token = token,
            authType = "docker",
            cacheKeyProvider = { dispatchService.redisKey(agentId, secretKey) },
            requiredFields = listOf("projectId", "pipelineId", "buildId", "vmName", "vmSeqId"),
            responseBuilder = { authInfo, _ ->
                AuthBuildResponse(
                    success = true,
                    projectId = authInfo.projectId,
                    pipelineId = authInfo.pipelineId,
                    buildId = authInfo.buildId,
                    agentId = agentId,
                    vmSeqId = authInfo.vmSeqId,
                    vmName = authInfo.vmName,
                    channelCode = authInfo.channelCode,
                    secretKey = secretKey
                )
            }
        )
    }

    /**
     * 插件构建机鉴权
     */
    fun authPlugin(
        secretKey: String,
        agentId: String,
        token: String
    ): AuthBuildResponse {
        return performAuth(
            token = token,
            authType = "plugin",
            cacheKeyProvider = { pluginAgentKey(agentId, secretKey) },
            requiredFields = listOf("projectId", "pipelineId", "buildId", "vmName", "vmSeqId"),
            responseBuilder = { authInfo, _ ->
                AuthBuildResponse(
                    success = true,
                    projectId = authInfo.projectId,
                    pipelineId = authInfo.pipelineId,
                    buildId = authInfo.buildId,
                    agentId = agentId,
                    vmSeqId = authInfo.vmSeqId,
                    vmName = authInfo.vmName,
                    channelCode = authInfo.channelCode,
                    secretKey = secretKey
                )
            }
        )
    }

    /**
     * MacOS构建机鉴权
     */
    fun authMacos(
        clientIp: String?,
        checkVersion: Boolean,
        token: String
    ): AuthBuildResponse {
        // 预先验证客户端IP
        if (clientIp.isNullOrEmpty()) {
            logger.warn("Client IP is null for macos auth")
            return createErrorResponse("Client IP is required")
        }

        return performAuth(
            token = token,
            authType = "macos",
            cacheKeyProvider = { macosKey(clientIp, checkVersion) },
            responseBuilder = { authInfo, _ ->
                val systemVersion = if (checkVersion) authInfo.systemVersion ?: "" else ""
                val xcodeVersion = if (checkVersion) authInfo.xcodeVersion ?: "" else ""

                AuthBuildResponse(
                    success = true,
                    projectId = authInfo.projectId,
                    pipelineId = authInfo.pipelineId,
                    buildId = authInfo.buildId,
                    agentId = authInfo.agentId,
                    vmSeqId = authInfo.vmSeqId,
                    vmName = authInfo.agentId,
                    channelCode = "",
                    secretKey = authInfo.secretKey,
                    systemVersion = systemVersion,
                    xcodeVersion = xcodeVersion
                )
            }
        )
    }

    /**
     * 其他构建机鉴权
     */
    fun authOther(
        clientIp: String?,
        token: String
    ): AuthBuildResponse {
        // 预先验证客户端IP
        if (clientIp.isNullOrEmpty()) {
            logger.warn("Client IP is null for other auth")
            return createErrorResponse("Client IP is required")
        }

        return performAuth(
            token = token,
            authType = "other",
            cacheKeyProvider = { clientIp },
            responseBuilder = { authInfo, _ ->
                AuthBuildResponse(
                    success = true,
                    projectId = authInfo.projectId,
                    pipelineId = authInfo.pipelineId,
                    buildId = authInfo.buildId,
                    agentId = "",
                    vmSeqId = authInfo.vmSeqId,
                    vmName = authInfo.vmName,
                    channelCode = authInfo.channelCode,
                    secretKey = ""
                )
            }
        )
    }

    /**
     * 创建错误响应
     */
    private fun createErrorResponse(message: String): AuthBuildResponse {
        return AuthBuildResponse(success = false, message = message)
    }

    /**
     * 从Redis获取并解析认证信息
     */
    private fun getAuthInfoFromCache(cacheKey: String): AuthBuildInfo? {
        val cacheValue = redisOperation.get(cacheKey)
        if (cacheValue.isNullOrEmpty()) {
            logger.warn("Redis cache not found for key: $cacheKey")
            return null
        }
        return objectMapper.readValue(cacheValue, AuthBuildInfo::class.java)
    }

    /**
     * 验证必需的字段
     */
    private fun validateRequiredFields(
        authInfo: AuthBuildInfo,
        cacheKey: String,
        requiredFields: List<String>
    ): AuthBuildResponse? {
        val fieldValidators = mapOf(
            "projectId" to { Pair(authInfo.projectId, "ProjectId is null") },
            "pipelineId" to { Pair(authInfo.pipelineId, "PipelineId is null") },
            "buildId" to { Pair(authInfo.buildId, "BuildId is null") },
            "vmSeqId" to { Pair(authInfo.vmSeqId, "VmSeqId is null") },
            "agentId" to { Pair(authInfo.agentId, "AgentId is null") },
            "vmName" to { Pair(authInfo.vmName, "VmName is null") }
        )

        for (fieldName in requiredFields) {
            fieldValidators[fieldName]?.let { validator ->
                val (fieldValue, errorMessage) = validator()
                if (fieldValue.isNullOrEmpty()) {
                    logger.warn("$errorMessage for key: $cacheKey")
                    return createErrorResponse(errorMessage)
                }
            }
        }
        return null
    }

    /**
     * 统一的鉴权处理方法
     */
    private fun performAuth(
        token: String,
        authType: String,
        cacheKeyProvider: () -> String?,
        requiredFields: List<String> = emptyList(),
        additionalValidation: ((AuthBuildInfo) -> AuthBuildResponse?)? = null,
        responseBuilder: (AuthBuildInfo, String) -> AuthBuildResponse
    ): AuthBuildResponse {
        return try {
            // 验证Token
            if (token != externalAuthToken) {
                logger.warn("Invalid token for $authType auth: $token")
                return createErrorResponse("Invalid token")
            }

            // 获取缓存Key
            val cacheKey = cacheKeyProvider() ?: return createErrorResponse("Invalid cache key")

            // 获取认证信息
            val authInfo = getAuthInfoFromCache(cacheKey)
                ?: return createErrorResponse("Authentication failed: cache not found")

            // 验证必需字段
            validateRequiredFields(authInfo, cacheKey, requiredFields)?.let { return it }

            // 额外的验证逻辑
            additionalValidation?.invoke(authInfo)?.let { return it }

            // 构建响应
            responseBuilder(authInfo, cacheKey)
        } catch (e: Exception) {
            logger.error("Error during $authType authentication", e)
            createErrorResponse("Authentication error: ${e.message}")
        }
    }

    /**
     * 生成插件构建机Redis key
     */
    private fun pluginAgentKey(agentId: String, secretKey: String) =
        "plugin_agent_${agentId}_$secretKey"

    /**
     * 生成MacOS构建机Redis key
     */
    private fun macosKey(clientIp: String, checkVersion: Boolean) =
        if (checkVersion) "dispatcher:devops_macos_$clientIp" else "devops_macos_$clientIp"
}
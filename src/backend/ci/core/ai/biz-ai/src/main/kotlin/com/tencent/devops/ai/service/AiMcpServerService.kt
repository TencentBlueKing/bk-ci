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

package com.tencent.devops.ai.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.ai.constant.AiMessageCode
import com.tencent.devops.ai.dao.AiMcpServerConfigDao
import com.tencent.devops.ai.pojo.AiMcpServerCreate
import com.tencent.devops.ai.pojo.AiMcpServerInfo
import com.tencent.devops.ai.pojo.AiMcpServerUpdate
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.ai.tables.records.TAiMcpServerConfigRecord
import io.agentscope.core.tool.mcp.McpClientBuilder
import io.agentscope.core.tool.mcp.McpClientWrapper
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZoneOffset

/**
 * MCP（Model Context Protocol）服务端配置管理。
 *
 * 支持系统级和用户级配置，同名 URL 时用户配置覆盖系统配置。
 */
@Service
class AiMcpServerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dao: AiMcpServerConfigDao
) {

    fun createServer(
        userId: String,
        request: AiMcpServerCreate
    ): AiMcpServerInfo {
        val id = UUIDUtil.generate()
        logger.info(
            "[McpServer] Creating: id={}, userId={}, name={}, url={}",
            id, userId, request.serverName, request.serverUrl
        )
        dao.create(
            dslContext = dslContext,
            id = id,
            scope = SCOPE_USER,
            userId = userId,
            serverName = request.serverName,
            serverUrl = request.serverUrl,
            transportType = request.transportType,
            headers = request.headers,
            bindAgent = request.bindAgent,
            enabled = request.enabled
        )
        val record = dao.getById(dslContext, id)
            ?: throw ErrorCodeException(
                errorCode = AiMessageCode.CREATE_MCP_SERVER_FAILED,
                defaultMessage = "Failed to create MCP server config"
            )
        return toInfo(record)
    }

    fun listForUser(userId: String): List<AiMcpServerInfo> {
        val merged = getMergedConfigs(userId)
        logger.info(
            "[McpServer] List for user: userId={}, total={}",
            userId, merged.size
        )
        return merged
    }

    fun updateServer(
        userId: String,
        serverId: String,
        request: AiMcpServerUpdate
    ): Boolean {
        logger.info(
            "[McpServer] Updating: userId={}, serverId={}",
            userId, serverId
        )
        checkOwnership(userId, serverId)
        val result = dao.update(
            dslContext = dslContext,
            id = serverId,
            serverName = request.serverName,
            serverUrl = request.serverUrl,
            transportType = request.transportType,
            headers = request.headers,
            bindAgent = request.bindAgent,
            enabled = request.enabled
        ) > 0
        return result
    }

    fun deleteServer(userId: String, serverId: String): Boolean {
        logger.info(
            "[McpServer] Deleting: userId={}, serverId={}",
            userId, serverId
        )
        checkOwnership(userId, serverId)
        val result = dao.delete(dslContext, serverId) > 0
        return result
    }

    /**
     * 合并系统级和用户级配置，同 URL 时用户配置优先。
     *
     * @param bindAgent 按目标智能体过滤；null 表示不过滤
     */
    fun getMergedConfigs(
        userId: String?,
        bindAgent: String? = null
    ): List<AiMcpServerInfo> {
        val systemConfigs = dao.listEnabled(
            dslContext, SCOPE_SYSTEM, bindAgent
        ).map { toInfo(it) }

        val userConfigs = if (userId != null) {
            dao.listEnabledByUser(
                dslContext, userId, bindAgent
            ).map { toInfo(it) }
        } else {
            emptyList()
        }

        val userUrls = userConfigs.map { it.serverUrl }.toSet()

        return systemConfigs.filter { it.serverUrl !in userUrls } + userConfigs
    }

    /**
     * 根据配置创建 MCP 客户端。
     * 每次调用都会新建连接，避免多实例间缓存不一致。
     */
    fun createClient(config: AiMcpServerInfo): McpClientWrapper {
        logger.info(
            "[McpServer] Building MCP client: name={}, " +
                    "transport={}, url={}",
            config.serverName,
            config.transportType,
            config.serverUrl
        )
        return buildMcpClient(config)
    }

    private fun buildMcpClient(config: AiMcpServerInfo): McpClientWrapper {
        val headers = parseHeaders(config.headers)
        val builder = McpClientBuilder.create(config.serverName)

        when (config.transportType) {
            TRANSPORT_SSE ->
                builder.sseTransport(config.serverUrl)

            TRANSPORT_STREAMABLE_HTTP ->
                builder.streamableHttpTransport(config.serverUrl)

            else -> {
                logger.warn(
                    "[McpServer] Unknown transport type: {}, " +
                            "falling back to SSE",
                    config.transportType
                )
                builder.sseTransport(config.serverUrl)
            }
        }

        headers.forEach { (k, v) -> builder.header(k, v) }
        builder.timeout(Duration.ofSeconds(MCP_TIMEOUT_SECONDS))

        return builder.buildAsync().block()
            ?: throw ErrorCodeException(
                errorCode = AiMessageCode.MCP_CLIENT_BUILD_FAILED,
                defaultMessage =
                    "Failed to build MCP client",
                params = arrayOf(config.serverName)
            )
    }

    private fun checkOwnership(userId: String, serverId: String) {
        val record = dao.getById(dslContext, serverId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.MCP_SERVER_NOT_FOUND,
                defaultMessage =
                    "MCP server config not found",
                params = arrayOf(serverId)
            )
        if (record.scope != SCOPE_USER || record.userId != userId) {
            logger.warn(
                "[McpServer] Permission denied: userId={} " +
                        "tried to operate config owned by " +
                        "scope={}, user={}",
                userId, record.scope, record.userId
            )
            throw ErrorCodeException(
                statusCode = 403,
                errorCode = AiMessageCode.MCP_SERVER_NO_PERMISSION,
                defaultMessage = "No permission",
                params = arrayOf(serverId)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiMcpServerService::class.java
        )
        private val objectMapper = jacksonObjectMapper()

        const val SCOPE_SYSTEM = "SYSTEM" // 系统级配置
        const val SCOPE_USER = "USER" // 用户级配置
        const val TRANSPORT_SSE = "SSE"
        const val TRANSPORT_STREAMABLE_HTTP = "STREAMABLE_HTTP"

        /** MCP 客户端连接超时（秒） */
        private const val MCP_TIMEOUT_SECONDS = 15L

        fun parseHeaders(headersJson: String?): Map<String, String> {
            if (headersJson.isNullOrBlank()) return emptyMap()
            return try {
                objectMapper.readValue(
                    headersJson,
                    object : TypeReference<Map<String, String>>() {}
                )
            } catch (e: Exception) {
                logger.warn(
                    "[McpServer] Failed to parse headers JSON: {}",
                    e.message
                )
                emptyMap()
            }
        }
    }

    private fun toInfo(record: TAiMcpServerConfigRecord): AiMcpServerInfo {
        return AiMcpServerInfo(
            id = record.id,
            scope = record.scope,
            userId = record.userId,
            serverName = record.serverName,
            serverUrl = record.serverUrl,
            transportType = record.transportType,
            headers = record.headers,
            bindAgent = record.bindAgent,
            enabled = record.enabled,
            createdTime = record.createdTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli(),
            updatedTime = record.updatedTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
        )
    }
}

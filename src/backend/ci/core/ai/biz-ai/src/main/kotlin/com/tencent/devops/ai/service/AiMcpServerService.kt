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
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
    private val dao: AiMcpServerConfigDao,
    private val accessTokenProvider: ObjectProvider<McpUserAccessTokenProvider>
) {
    /**
     * CodeCC 首次生成修复建议可能超过原有 15 秒，其余既有 MCP 继续使用原超时。
     */
    @Value("\${ai.codecc.mcp-timeout-seconds:60}")
    private var codeccMcpTimeoutSeconds: Long = DEFAULT_CODECC_MCP_TIMEOUT_SECONDS

    fun createSystem(request: AiMcpServerCreate): AiMcpServerInfo {
        val id = UUIDUtil.generate()
        dao.create(
            dslContext = dslContext,
            id = id,
            scope = SCOPE_SYSTEM,
            userId = null,
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

    fun listAllForOp(): List<AiMcpServerInfo> {
        return dao.listAll(dslContext).map { toInfo(it) }
    }

    fun updateForOp(serverId: String, request: AiMcpServerUpdate): Boolean {
        dao.getById(dslContext, serverId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.MCP_SERVER_NOT_FOUND,
                defaultMessage = "MCP server config not found",
                params = arrayOf(serverId)
            )
        return dao.update(
            dslContext = dslContext,
            id = serverId,
            serverName = request.serverName,
            serverUrl = request.serverUrl,
            transportType = request.transportType,
            headers = request.headers,
            bindAgent = request.bindAgent,
            enabled = request.enabled
        ) > 0
    }

    fun deleteForOp(serverId: String): Boolean {
        dao.getById(dslContext, serverId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.MCP_SERVER_NOT_FOUND,
                defaultMessage = "MCP server config not found",
                params = arrayOf(serverId)
            )
        return dao.delete(dslContext, serverId) > 0
    }

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
        /*val merged = getMergedConfigs(userId)
        logger.info(
            "[McpServer] List for user: userId={}, total={}",
            userId, merged.size
        )
        return merged*/
        return emptyList()
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
     *
     * @param operator 当前会话用户。仅当某个配置头显式包含 `{{userId}}` 或 `{{accessToken}}` 时才使用，
     *        未声明占位符的既有 MCP 配置保持完全不变
     */
    fun createClient(config: AiMcpServerInfo, operator: String? = null): McpClientWrapper {
        logger.info(
            "[McpServer] Building MCP client: name={}, " +
                    "transport={}, url={}, operator={}",
            config.serverName,
            config.transportType,
            config.serverUrl,
            operator
        )
        return buildMcpClient(config, operator)
    }

    private fun buildMcpClient(config: AiMcpServerInfo, operator: String?): McpClientWrapper {
        val headers = resolveHeaders(config, operator, ::loadAccessToken)
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
        val timeoutSeconds = if (config.bindAgent == CODECC_AGENT) {
            codeccMcpTimeoutSeconds.coerceIn(MIN_MCP_TIMEOUT_SECONDS, MAX_MCP_TIMEOUT_SECONDS)
        } else {
            DEFAULT_MCP_TIMEOUT_SECONDS
        }
        builder.timeout(
            Duration.ofSeconds(timeoutSeconds)
        )

        return builder.buildAsync().block()
            ?: throw ErrorCodeException(
                errorCode = AiMessageCode.MCP_CLIENT_BUILD_FAILED,
                defaultMessage =
                    "Failed to build MCP client",
                params = arrayOf(config.serverName)
            )
    }

    private fun loadAccessToken(userId: String): String? {
        val provider = accessTokenProvider.ifAvailable ?: run {
            logger.warn("[McpServer] No McpUserAccessTokenProvider, userId={}", userId)
            return null
        }
        return try {
            provider.getAccessToken(userId)
        } catch (e: Exception) {
            logger.warn("[McpServer] Failed to get access token: userId={}, reason={}", userId, e.message)
            null
        }
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

        private const val UNKNOWN_OPERATOR = "unknown"
        private const val USER_ID_PLACEHOLDER = "{{userId}}"
        private const val ACCESS_TOKEN_PLACEHOLDER = "{{accessToken}}"
        private const val MAX_OPERATOR_LENGTH = 128

        /**
         * 令牌会被插入到 X-Bkapi-Authorization 的 JSON 字符串中，只接受不含引号、反斜杠等字符的值，
         * 防止通过令牌内容注入额外的认证字段
         */
        private val ACCESS_TOKEN_PATTERN = Regex("^[A-Za-z0-9._~+/=-]{1,4096}$")

        private const val CODECC_AGENT = "codecc_agent"
        private const val DEFAULT_MCP_TIMEOUT_SECONDS = 15L
        private const val DEFAULT_CODECC_MCP_TIMEOUT_SECONDS = 60L
        private const val MIN_MCP_TIMEOUT_SECONDS = 5L
        private const val MAX_MCP_TIMEOUT_SECONDS = 300L

        /**
         * 组装 MCP 请求头。
         *
         * 只有显式写入 `{{userId}}` / `{{accessToken}}` 的 header 才注入会话用户身份。
         * 这样 CodeCC 可以在 X-Bkapi-Authorization 中以用户 access_token 完成网关认证，
         * iWiki 等既有 MCP 的静态 PAT/header 不会被改写或附加额外字段。
         *
         * @param accessTokenLoader 按会话用户获取 access_token，仅在配置声明 `{{accessToken}}` 时调用
         */
        fun resolveHeaders(
            config: AiMcpServerInfo,
            operator: String?,
            accessTokenLoader: ((String) -> String?)? = null
        ): Map<String, String> {
            val headers = parseHeaders(config.headers)
            val needsUser = headers.values.any { it.contains(USER_ID_PLACEHOLDER) }
            val needsAccessToken = headers.values.any { it.contains(ACCESS_TOKEN_PLACEHOLDER) }
            if (!needsUser && !needsAccessToken) {
                return headers
            }
            val safeOperator = operator
                ?.replace(Regex("[\\r\\n\\u0000-\\u001F]"), " ")
                ?.trim()
                ?.take(MAX_OPERATOR_LENGTH)
                ?.takeIf { it.isNotEmpty() && it != UNKNOWN_OPERATOR }
                ?: throw IllegalStateException(
                    "MCP config ${config.serverName} requires an authenticated user"
                )
            val safeAccessToken = if (needsAccessToken) {
                accessTokenLoader?.invoke(safeOperator)?.trim()?.takeIf { ACCESS_TOKEN_PATTERN.matches(it) }
                    ?: throw IllegalStateException(
                        "MCP config ${config.serverName} requires a valid access token of user $safeOperator"
                    )
            } else {
                null
            }
            return headers.mapValues { (_, value) ->
                var resolved = value.replace(USER_ID_PLACEHOLDER, safeOperator)
                safeAccessToken?.let { resolved = resolved.replace(ACCESS_TOKEN_PLACEHOLDER, it) }
                resolved
            }
        }

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

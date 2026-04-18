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

package com.tencent.devops.ai.agent

import com.tencent.devops.auth.api.service.ServiceAuthAiResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import io.agentscope.core.tool.Tool
import io.agentscope.core.tool.ToolParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.function.Supplier

/**
 * 公共工具集，提供项目查询、资源查询、用户中文名解析等通用能力。
 */
class CommonTools(
    client: Client,
    userIdSupplier: Supplier<String>
) : BaseTools(client, userIdSupplier) {

    override val logger: Logger = LoggerFactory.getLogger(CommonTools::class.java)

    private fun authAiResource() = service(ServiceAuthAiResource::class)

    @Tool(
        name = "获取当前时间",
        description = "获取服务端当前时间，返回 ISO 8601 格式字符串。" +
            "用于处理今天、现在、是否过期、续期到期时间等时间相关问题。"
    )
    fun getCurrentTime(): String {
        return safeQuery("CommonTools", "getCurrentTime") {
            OffsetDateTime.now(Clock.systemDefaultZone()).toString()
        }
    }

    @Tool(
        name = "查询项目信息",
        description = "根据项目名称或英文标识查询项目信息，返回项目的英文标识(projectId)和显示名称。" +
            "当用户提供的是项目显示名称而非英文标识时，必须先调用此工具获取 projectId，再执行后续操作。"
    )
    fun resolveProjectId(
        @ToolParam(
            name = "projectNameOrCode",
            description = "项目名称（中文显示名）或英文标识（projectId）"
        )
        projectNameOrCode: String
    ): String {
        val userId = getOperatorUserId()
        return safeQuery("CommonTools", "resolveProjectId") {
            val projectResource = service(ServiceProjectResource::class)
            val byCode = try {
                projectResource.get(projectNameOrCode).data
            } catch (_: Exception) {
                null
            }
            if (byCode != null) {
                return@safeQuery "项目: ${byCode.englishName} (${byCode.projectName})"
            }
            val byName = try {
                projectResource.getProjectByName(
                    userId = userId,
                    projectName = projectNameOrCode
                ).data
            } catch (_: Exception) {
                null
            }
            if (byName != null) {
                return@safeQuery "项目: ${byName.englishName} (${byName.projectName})"
            }
            "未找到名为「$projectNameOrCode」的项目，请确认名称或英文标识是否正确。"
        }
    }

    @Tool(
        name = "解析用户中文名",
        description = "根据用户中文显示名（如姓名）查询所有匹配的英文 userId。" +
                "当用户提供「张三」等中文名而非英文账号时，应先调用本工具解析，再用于权限查询、交接人等后续操作。" +
            "数据来自平台同步的用户信息表，按姓名精确匹配；同一中文名可能对应多名用户，本工具会全部列出。" +
                "若查无此人，请让用户提供英文 userId，或在有项目上下文时使用搜索用户工具。"
    )
    fun resolveUserIdByName(
        @ToolParam(name = "userName", description = "用户中文显示名（姓名等）")
        userName: String
    ): String {
        return safeQuery("CommonTools", "resolveUserIdByName") {
            val users = authAiResource().resolveUsersByName(userName.trim()).data ?: emptyList()
            if (users.isEmpty()) {
                return@safeQuery "未找到中文名为「$userName」的用户，请确认姓名或直接使用英文 userId。"
            }
            buildString {
                appendLine("中文名「$userName」共匹配 ${users.size} 个用户：")
                users.forEachIndexed { index, u ->
                    val dept = u.departmentName?.let { " | 部门: $it" } ?: ""
                    val status = when {
                        u.departed -> " | 状态: 已离职"
                        !u.enabled -> " | 状态: 已禁用"
                        else -> ""
                    }
                    appendLine("${index + 1}. userId: ${u.userId}$dept$status")
                }
                if (users.size > 1) {
                    appendLine("存在重名，请结合部门等信息让用户确认具体 userId，再继续后续操作。")
                }
            }.trimEnd()
        }
    }

    @Tool(
        name = "查询资源信息",
        description = "根据资源名称或Code查询资源信息，返回资源的 Code、显示名称等。" +
            "适用于非项目类资源：pipeline、credential 等。" +
            "projectId 必须提供（请先通过 resolveProjectId 获取）。" +
            "无论用户提供的是名称还是Code，都可以调用此工具查询确认。"
    )
    fun resolveResource(
        @ToolParam(name = "projectId", description = "项目ID（英文标识），必须提供")
        projectId: String,
        @ToolParam(
            name = "resourceType",
            description = "资源类型标识，如 pipeline、credential、repertory、environment 等"
        )
        resourceType: String,
        @ToolParam(name = "keyword", description = "资源的显示名称或Code均可")
        keyword: String
    ): String {
        return safeQuery("CommonTools", "resolveResource") {
            val resources = authAiResource().searchResource(
                userId = getOperatorUserId(), projectId = projectId,
                resourceType = resourceType, keyword = keyword
            ).data ?: emptyList()
            if (resources.isEmpty()) {
                return@safeQuery "未找到项目 $projectId 中类型为 $resourceType 匹配「$keyword」的资源。"
            }
            resources.joinToString("\n") { r -> "- ${r.resourceName} (${r.resourceCode})" }
        }
    }

    @Tool(
        name = "获取资源类型列表",
        description = "获取所有资源类型列表（如流水线、代码库等）。用于了解系统支持哪些资源类型。"
    )
    fun listResourceTypes(): String {
        return safeQuery("CommonTools", "listResourceTypes") {
            val types = authAiResource().listResourceTypes(userId = getOperatorUserId()).data ?: emptyList()
            if (types.isEmpty()) return@safeQuery "暂无资源类型"
            types.joinToString("\n") { t -> "- ${t.resourceType}: ${t.name}" }
        }
    }

    @Tool(
        name = "获取资源操作权限列表",
        description = "获取指定资源类型的操作权限列表。例如流水线有'查看'、'执行'、'编辑'等操作。"
    )
    fun listActions(
        @ToolParam(name = "resourceType", description = "资源类型标识，如 pipeline、code_repo 等")
        resourceType: String
    ): String {
        return safeQuery("CommonTools", "listActions") {
            val actions = authAiResource().listActions(
                userId = getOperatorUserId(), resourceType = resourceType
            ).data ?: emptyList()
            if (actions.isEmpty()) return@safeQuery "资源类型 $resourceType 暂无操作权限定义"
            actions.joinToString("\n") { a -> "- ${a.action}: ${a.actionName}" }
        }
    }

    @Tool(name = "按名称查询资源", description = "根据资源名称查询资源信息。如根据流水线名称获取其Code和ID。")
    fun getResourceByName(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "resourceType", description = "资源类型标识")
        resourceType: String,
        @ToolParam(name = "resourceName", description = "资源名称")
        resourceName: String
    ): String {
        return safeQuery("CommonTools", "getResourceByName") {
            val info = authAiResource().getResourceByName(
                userId = getOperatorUserId(), projectId = projectId,
                resourceType = resourceType, resourceName = resourceName
            ).data ?: return@safeQuery "未找到资源: $resourceName"
            "资源: ${info.resourceName} (Code: ${info.resourceCode}, 类型: ${info.resourceType})"
        }
    }

    @Tool(name = "按Code查询资源", description = "根据资源Code查询资源信息。如根据流水线Code获取其名称和ID。")
    fun getResourceByCode(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "resourceType", description = "资源类型标识")
        resourceType: String,
        @ToolParam(name = "resourceCode", description = "资源Code")
        resourceCode: String
    ): String {
        return safeQuery("CommonTools", "getResourceByCode") {
            val info = authAiResource().getResourceByCode(
                userId = getOperatorUserId(), projectId = projectId,
                resourceType = resourceType, resourceCode = resourceCode
            ).data ?: return@safeQuery "未找到资源: $resourceCode"
            "资源: ${info.resourceName} (Code: ${info.resourceCode}, 类型: ${info.resourceType})"
        }
    }
}

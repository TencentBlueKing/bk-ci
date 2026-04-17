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

package com.tencent.devops.ai.agent.build

import com.tencent.devops.ai.agent.BaseTools
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import io.agentscope.core.tool.Tool
import io.agentscope.core.tool.ToolParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * 流水线构建与制品工具集，提供流水线查询、构建操作、日志获取、制品下载等能力。
 */
@Suppress("TooManyFunctions")
class BuildTools(
    client: Client,
    userIdSupplier: Supplier<String>
) : BaseTools(client, userIdSupplier) {

    override val logger: Logger = LoggerFactory.getLogger(BuildTools::class.java)

    private fun pipelineResource() = service(ServicePipelineResource::class)
    private fun buildResource() = service(ServiceBuildResource::class)
    private fun logResource() = service(ServiceLogResource::class)

    // ── 流水线查询 ──

    @Tool(
        name = "搜索流水线",
        description = "按名称搜索流水线，返回匹配的流水线列表（ID、名称、最新构建状态等）。" +
            "keyword 为空时返回项目下所有流水线（分页）。支持分页查询。"
    )
    fun searchPipelines(
        @ToolParam(name = "projectId", description = "项目ID（英文标识）")
        projectId: String,
        @ToolParam(name = "keyword", description = "流水线名称关键字（可选）", required = false)
        keyword: String? = null,
        @ToolParam(name = "page", description = "页码，默认1", required = false)
        page: Int? = null,
        @ToolParam(name = "pageSize", description = "每页条数，默认10", required = false)
        pageSize: Int? = null
    ): String {
        return safeQuery("BuildArtifactTool", "searchPipelines") {
            val actualPageSize = (pageSize ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
            val result = pipelineResource().pagingSearchByName(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineName = keyword,
                page = page ?: 1,
                pageSize = actualPageSize
            )
            val data = result.data ?: return@safeQuery "搜索失败"
            val records = data.records
            if (records.isEmpty()) return@safeQuery "未找到匹配的流水线"
            toJson(data)
        }
    }

    @Tool(
        name = "获取流水线信息",
        description = "获取流水线的基本信息，包括名称、创建者、最新版本等。"
    )
    fun getPipelineInfo(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String
    ): String {
        return safeQuery("BuildArtifactTool", "getPipelineInfo") {
            val result = pipelineResource().getPipelineInfo(
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = ChannelCode.BS
            )
            val info = result.data ?: return@safeQuery "未找到流水线 $pipelineId"
            toJson(info)
        }
    }

    @Tool(
        name = "获取流水线状态",
        description = "获取流水线的当前运行状态，包括最近一次构建的状态、构建号等。"
    )
    fun getPipelineStatus(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String
    ): String {
        return safeQuery("BuildArtifactTool", "getPipelineStatus") {
            val result = pipelineResource().status(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = ChannelCode.BS
            )
            val pipeline = result.data ?: return@safeQuery "未找到流水线 $pipelineId"
            toJson(pipeline)
        }
    }

    // ── 构建操作 ──

    @Tool(
        name = "获取手动启动参数",
        description = "获取流水线的手动启动参数列表，包括参数名、类型、默认值、可选值等。" +
            "触发构建前建议先调用此工具了解可配置的参数。"
    )
    fun getManualStartupInfo(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String
    ): String {
        return safeQuery("BuildArtifactTool", "getManualStartupInfo") {
            val result = buildResource().manualStartupInfo(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = ChannelCode.BS
            )
            val info = result.data ?: return@safeQuery "获取启动参数失败"
            toJson(info)
        }
    }

    @Tool(
        name = "触发构建",
        description = "手动触发流水线构建。这是写操作，执行前必须向用户确认。" +
            "params 为启动参数的 JSON 字符串，格式如 {\"key1\":\"value1\",\"key2\":\"value2\"}，" +
            "可传空字符串或不传使用默认值。"
    )
    fun triggerBuild(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(
            name = "params",
            description = "启动参数JSON字符串，如 {\"key\":\"value\"}（可选，不传使用默认值）",
            required = false
        )
        params: String? = null
    ): String {
        return safeOperate(
            "BuildArtifactTool",
            "triggerBuild",
            mapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "params" to params
            )
        ) {
            val values: Map<String, String> = if (params.isNullOrBlank()) {
                emptyMap()
            } else {
                try {
                    @Suppress("UNCHECKED_CAST")
                    JsonUtil.to(params, Map::class.java) as Map<String, String>
                } catch (e: Exception) {
                    return@safeOperate "启动参数格式错误，请使用 JSON 格式如 {\"key\":\"value\"}: ${e.message}"
                }
            }
            val result = buildResource().manualStartupNew(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                values = values,
                channelCode = ChannelCode.BS,
                startType = StartType.SERVICE
            )
            val buildId = result.data ?: return@safeOperate "触发构建失败: ${result.message}"
            "构建已触发成功，构建ID: ${buildId.id}"
        }
    }

    @Tool(
        name = "重试构建",
        description = "重试失败的构建，默认仅重试失败的 Job。这是写操作，执行前必须向用户确认。"
    )
    fun retryBuild(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "buildId", description = "构建ID")
        buildId: String
    ): String {
        return safeOperate(
            "BuildArtifactTool",
            "retryBuild",
            mapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "buildId" to buildId
            )
        ) {
            val result = buildResource().retry(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                failedContainer = true,
                channelCode = ChannelCode.BS
            )
            val newBuildId = result.data ?: return@safeOperate "重试失败: ${result.message}"
            "重试已触发，新构建ID: ${newBuildId.id}"
        }
    }

    @Tool(
        name = "停止构建",
        description = "手动停止正在运行的构建。这是写操作，执行前必须向用户确认。"
    )
    fun stopBuild(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "buildId", description = "构建ID")
        buildId: String
    ): String {
        return safeOperate(
            "BuildArtifactTool",
            "stopBuild",
            mapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "buildId" to buildId
            )
        ) {
            val result = buildResource().manualShutdown(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.BS
            )
            if (result.data == true) {
                "构建 $buildId 已停止"
            } else {
                "停止构建失败: ${result.message}"
            }
        }
    }

    // ── 构建查询 ──

    @Tool(
        name = "获取构建历史",
        description = "获取流水线的构建历史列表。支持按状态、执行人筛选。"
    )
    fun getBuildHistory(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "page", description = "页码，默认1", required = false)
        page: Int? = null,
        @ToolParam(name = "pageSize", description = "每页条数，默认10", required = false)
        pageSize: Int? = null,
        @ToolParam(
            name = "status",
            description = "构建状态过滤，如 SUCCEED/FAILED/RUNNING/CANCELED（可选，逗号分隔多个）",
            required = false
        )
        status: String? = null,
        @ToolParam(name = "startUser", description = "执行人过滤（可选，逗号分隔多个）", required = false)
        startUser: String? = null
    ): String {
        return safeQuery("BuildArtifactTool", "getBuildHistory") {
            val statusList = status?.let {
                parseCommaSeparated(it).map { s ->
                    com.tencent.devops.common.pipeline.enums.BuildStatus.valueOf(s.uppercase())
                }
            }
            val userList = startUser?.let { parseCommaSeparated(it) }
            val actualPageSize = (pageSize ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
            val result = buildResource().getHistoryBuild(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                page = page ?: 1,
                pageSize = actualPageSize,
                channelCode = ChannelCode.BS,
                status = statusList,
                startUser = userList
            )
            val data = result.data ?: return@safeQuery "获取构建历史失败"
            if (data.records.isEmpty()) return@safeQuery "暂无构建记录"
            toJson(data)
        }
    }

    @Tool(
        name = "获取构建详情",
        description = "获取构建的完整详情，包含流水线编排（stages/containers/elements）和各插件执行状态。" +
            "用于定位失败的插件 element ID，以便后续查询该插件的日志。"
    )
    fun getBuildDetail(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "buildId", description = "构建ID")
        buildId: String
    ): String {
        return safeQuery("BuildArtifactTool", "getBuildDetail") {
            val result = buildResource().getBuildDetail(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.BS
            )
            val detail = result.data ?: return@safeQuery "未找到构建 $buildId"
            toJson(detail)
        }
    }

    @Tool(
        name = "获取构建状态",
        description = "获取构建的当前状态信息，包含状态、启动时间、结束时间、触发人等。"
    )
    fun getBuildStatus(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "buildId", description = "构建ID")
        buildId: String
    ): String {
        return safeQuery("BuildArtifactTool", "getBuildStatus") {
            val result = buildResource().getBuildStatus(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.BS
            )
            val status = result.data ?: return@safeQuery "未找到构建 $buildId"
            toJson(status)
        }
    }

    @Tool(
        name = "获取构建变量",
        description = "获取构建的全部变量值，包括流水线变量和构建产生的变量。"
    )
    fun getBuildVars(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "buildId", description = "构建ID")
        buildId: String
    ): String {
        return safeQuery("BuildArtifactTool", "getBuildVars") {
            val result = buildResource().getBuildVars(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.BS
            )
            val vars = result.data ?: return@safeQuery "未找到构建 $buildId"
            toJson(vars)
        }
    }

    // ── 日志分析 ──

    @Tool(
        name = "获取构建日志",
        description = "获取构建日志。强烈建议传入 tag 参数（即 elementId，格式 e-xxxxxxxx）定位到具体插件，" +
            "避免返回全量日志导致内容过大。" +
            "如果日志内容不完整或包含「Please download logs to view.」标记，" +
            "说明日志触发了熔断，应提醒用户到蓝盾页面下载完整日志。"
    )
    fun getBuildLogs(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "buildId", description = "构建ID")
        buildId: String,
        @ToolParam(
            name = "tag",
            description = "对应 elementId（格式 e-xxxxxxxx），用于定位具体插件的日志（强烈建议提供）",
            required = false
        )
        tag: String? = null,
        @ToolParam(name = "stepId", description = "对应 stepId（可选）", required = false)
        stepId: String? = null,
        @ToolParam(name = "jobId", description = "对应 jobId（可选）", required = false)
        jobId: String? = null
    ): String {
        return safeQuery("BuildArtifactTool", "getBuildLogs") {
            val result = logResource().getInitLogs(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                tag = tag,
                containerHashId = null,
                executeCount = null,
                jobId = jobId,
                stepId = stepId
            )
            val logs = result.data ?: return@safeQuery "获取日志失败"
            toJson(logs)
        }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val MAX_PAGE_SIZE = 50
    }
}

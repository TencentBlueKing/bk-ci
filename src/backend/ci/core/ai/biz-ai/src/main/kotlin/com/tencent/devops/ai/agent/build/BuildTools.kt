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

package com.tencent.devops.ai.agent.build

import com.tencent.devops.ai.agent.BaseTools
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.QueryLogsText
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineVersionResource
import io.agentscope.core.tool.Tool
import io.agentscope.core.tool.ToolParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * 流水线构建与制品工具集，提供流水线查询、构建操作、日志获取、制品下载等能力。
 */
@Suppress("TooManyFunctions", "ComplexCondition")
class BuildTools(
    client: Client,
    userIdSupplier: Supplier<String>
) : BaseTools(client, userIdSupplier) {

    override val logger: Logger = LoggerFactory.getLogger(BuildTools::class.java)

    private fun pipelineResource() = service(ServicePipelineResource::class)
    private fun versionResource() = service(ServicePipelineVersionResource::class)
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
            val resultMap = JsonUtil.toMutableMap(data)
            resultMap["records"] = records.map { record ->
                JsonUtil.toMutableMap(record).apply {
                    this["pipelineDetailUrl"] = buildPipelineDetailUrl(
                        projectId = record.projectId,
                        pipelineId = record.pipelineId
                    )
                }
            }
            toJson(resultMap)
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
        description = "获取流水线当前状态信息，包括最近一次构建状态、构建号和阶段状态等。"
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

    @Tool(
        name = "获取流水线编排",
        description = "获取流水线的编排 Model，包括阶段、任务、参数等。" +
                "支持按指定版本号查询；version 不传时默认返回最新正式版本。"
    )
    fun getPipelineModel(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "version", description = "流水线版本号（可选，不传默认最新）", required = false)
        version: Int? = null
    ): String {
        return safeQuery("BuildArtifactTool", "getPipelineModel") {
            val result = versionResource().getVersionModel(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            )
            val data = result.data ?: return@safeQuery "未找到流水线 $pipelineId 的编排信息"
            toJson(
                mapOf(
                    "pipelineId" to pipelineId,
                    "version" to data.version,
                    "versionName" to data.versionName,
                    "latestVersion" to data.latestVersion,
                    "model" to data.modelAndSetting.model,
                    "setting" to data.modelAndSetting.setting
                )
            )
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
            description = "启动参数JSON字符串，如 [{\"key1\":\"value1\"},{\"key2\":\"value2\"}]（可选，不传使用默认值）",
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
                    JsonUtil.to<Map<String, String>>(params)
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
        description = "获取构建的 AI 简化详情。返回顶层摘要信息，以及 failedElements 列表。" +
                "failedElements 的每一项都包含 stageId、stageName、containerId、containerName、containerHashId、jobId，" +
                "以及完整的 element 对象。" +
                "用于定位失败插件的 element.id，并结合完整插件配置继续排查日志和失败原因。"
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
            val result = buildResource().getBuildDetailSimple(
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
        description = "获取构建最新日志窗口，专用于 AI 构建报错分析。" +
                "内部调用日志服务 latest 接口，只返回当前查询条件下行号最大的 N 条日志。" +
                "强烈建议传入 tag 参数（即 elementId，格式 e-xxxxxxxx）定位到具体插件，避免查询全量日志。" +
                "排查报错时可传 logType=ERROR 获取错误日志，同时也应获取 logType 为空的普通日志作为上下文。" +
                "如果最新窗口不足以判断根因，请根据返回的 lineRange 调用「获取指定行号范围构建日志」继续向前滚动。"
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
        @ToolParam(
            name = "logType",
            description = "日志级别过滤（可选），支持 WARN/ERROR/DEBUG/LOG",
            required = false
        )
        logType: String? = null,
        @ToolParam(name = "jobId", description = "对应 jobId（可选）", required = false)
        jobId: String? = null,
        @ToolParam(
            name = "size",
            description = "返回最新日志条数，默认 500，最大 10000",
            required = false
        )
        size: Int? = null
    ): String {
        return safeQuery("BuildArtifactTool", "getBuildLogs") {
            val actualLogType = if (logType.isNullOrBlank()) {
                null
            } else {
                parseLogType(logType)
                    ?: return@safeQuery "logType 无效，支持的值为 WARN、ERROR、DEBUG、LOG"
            }
            val debug = actualLogType == LogType.DEBUG
            val actualSize = (size ?: DEFAULT_LOG_SIZE).coerceIn(1, MAX_LOG_LINES)
            val result = logResource().getLatestLogs(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                debug = debug,
                logType = actualLogType,
                size = actualSize,
                tag = tag,
                subTag = null,
                containerHashId = null,
                executeCount = null,
                jobId = jobId,
                stepId = stepId,
                archiveFlag = null
            )
            val latestLogs = result.data ?: return@safeQuery "获取日志失败"
            buildTextLogResult(
                buildId = buildId,
                tag = tag,
                jobId = jobId,
                stepId = stepId,
                logType = actualLogType?.name,
                logsText = latestLogs,
                notices = buildLatestLogNotices(
                    logsText = latestLogs,
                    tag = tag,
                    content = latestLogs.content
                )
            )
        }
    }

    @Tool(
        name = "获取指定行号范围构建日志",
        description = "按行号范围获取构建日志，专用于 AI 在 latest 最新窗口不足时继续滚动拉取上下文。" +
                "内部调用日志服务 middle 接口，start/end 区间最多 10000 行。" +
                "应根据「获取构建日志」返回的 lineRange 或可疑行号继续向前/向后取窗口。"
    )
    fun getMiddleBuildLogs(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "buildId", description = "构建ID")
        buildId: String,
        @ToolParam(name = "start", description = "起始行号，必须大于 0")
        start: Long,
        @ToolParam(name = "end", description = "结束行号，必须大于等于 start，区间最多 10000 行")
        end: Long,
        @ToolParam(
            name = "tag",
            description = "对应 elementId（格式 e-xxxxxxxx），用于定位具体插件的日志（强烈建议提供）",
            required = false
        )
        tag: String? = null,
        @ToolParam(name = "stepId", description = "对应 stepId（可选）", required = false)
        stepId: String? = null,
        @ToolParam(
            name = "logType",
            description = "日志级别过滤（可选），支持 WARN/ERROR/DEBUG/LOG",
            required = false
        )
        logType: String? = null,
        @ToolParam(name = "jobId", description = "对应 jobId（可选）", required = false)
        jobId: String? = null
    ): String {
        return safeQuery("BuildArtifactTool", "getMiddleBuildLogs") {
            val actualLogType = if (logType.isNullOrBlank()) {
                null
            } else {
                parseLogType(logType)
                    ?: return@safeQuery "logType 无效，支持的值为 WARN、ERROR、DEBUG、LOG"
            }
            val debug = actualLogType == LogType.DEBUG
            val result = logResource().getMiddleLogs(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                start = start,
                end = end,
                debug = debug,
                logType = actualLogType,
                tag = tag,
                subTag = null,
                containerHashId = null,
                executeCount = null,
                jobId = jobId,
                stepId = stepId,
                archiveFlag = null
            )
            val middleLogs = result.data ?: return@safeQuery "获取指定范围日志失败"
            buildTextLogResult(
                buildId = buildId,
                tag = tag,
                jobId = jobId,
                stepId = stepId,
                logType = actualLogType?.name,
                logsText = middleLogs,
                notices = buildMiddleLogNotices(
                    logsText = middleLogs,
                    tag = tag,
                    content = middleLogs.content
                )
            )
        }
    }

    @Tool(
        name = "分析构建失败",
        description = "一键排查构建失败原因：自动定位失败插件，并抓取 latest 错误日志和 latest 普通日志。" +
                "buildId 不传时默认分析该流水线最新一次构建。" +
                "这是分析流水线报错的默认首选工具；若日志不足以判断根因，再调用 middle 范围日志工具继续滚动。"
    )
    fun analyzeBuildFailure(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String,
        @ToolParam(name = "buildId", description = "构建ID（可选，不传默认分析最新一次构建）", required = false)
        buildId: String? = null
    ): String {
        return safeQuery("BuildArtifactTool", "analyzeBuildFailure") {
            val actualBuildId = buildId?.takeIf { it.isNotBlank() } ?: run {
                val history = buildResource().getHistoryBuild(
                    userId = getOperatorUserId(),
                    projectId = projectId,
                    pipelineId = pipelineId,
                    page = 1,
                    pageSize = 1,
                    channelCode = ChannelCode.BS
                ).data
                history?.records?.firstOrNull()?.id
                    ?: return@safeQuery "未找到流水线 $pipelineId 的构建记录"
            }
            val detail = buildResource().getBuildDetailSimple(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = actualBuildId,
                channelCode = ChannelCode.BS
            ).data ?: return@safeQuery "未找到构建 $actualBuildId"

            val summary = linkedMapOf<String, Any?>(
                "buildId" to actualBuildId,
                "buildNum" to detail.buildNum,
                "status" to detail.status,
                "failedElementCount" to detail.failedElementCount,
                "stageSummary" to detail.stageSummary,
                "detailUrl" to buildBuildDetailUrl(projectId, pipelineId, actualBuildId)
            )

            if (detail.failedElements.isEmpty()) {
                summary["message"] =
                    "未发现失败插件。可能构建未失败，或失败发生在触发/环境准备阶段，请结合 stageSummary 判断。"
                summary["notices"] = detail.notices
                return@safeQuery toJson(summary)
            }

            val analyzed = detail.failedElements.take(MAX_FAILED_ELEMENTS_TO_ANALYZE).map { fe ->
                linkedMapOf(
                    "stageName" to fe.stageName,
                    "jobName" to fe.containerName,
                    "jobId" to fe.jobId,
                    "elementId" to fe.elementId,
                    "elementName" to fe.elementName,
                    "stepId" to fe.stepId,
                    "status" to fe.status,
                    "errorType" to fe.errorType,
                    "errorCode" to fe.errorCode,
                    "errorMsg" to fe.errorMsg,
                    "element" to fe.element,
                    "errorLog" to fetchFailureLog(projectId, pipelineId, actualBuildId, fe.elementId, fe.jobId)
                )
            }
            summary["analyzedElementCount"] = analyzed.size
            summary["failedElements"] = analyzed
            if (detail.failedElements.size > analyzed.size) {
                summary["notices"] = listOf(
                    "失败插件较多，仅分析前 ${analyzed.size} 个；如需其余插件日志请指定 elementId 单独查询。"
                )
            }
            toJson(summary)
        }
    }

    /**
     * 抓取单个失败插件的 AI 日志上下文：ERROR 最新日志和普通最新日志都返回。
     * 如果仍无法判断根因，由返回结果提示 AI 继续调用 middle 范围日志工具滚动拉取更多上下文。
     */
    private fun fetchFailureLog(
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String?,
        jobId: String?
    ): Any {
        if (elementId.isNullOrBlank()) {
            return "该失败插件缺少 elementId，无法定位日志，请结合构建详情人工排查。"
        }
        val errorLogJson = getBuildLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            tag = elementId,
            stepId = null,
            logType = "ERROR",
            jobId = jobId,
            size = FAILURE_LOG_SIZE
        )
        val latestLogJson = getBuildLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            tag = elementId,
            stepId = null,
            logType = null,
            jobId = jobId,
            size = FAILURE_LOG_SIZE
        )
        val errorLog = runCatching { JsonUtil.toMutableMap(errorLogJson) }.getOrElse {
            linkedMapOf<String, Any?>("raw" to errorLogJson)
        }
        val latestLog = runCatching { JsonUtil.toMutableMap(latestLogJson) }.getOrElse {
            linkedMapOf<String, Any?>("raw" to latestLogJson)
        }
        return linkedMapOf(
            "errorLatestLog" to errorLog,
            "latestLog" to latestLog,
            "nextActions" to buildMiddleLogNextActions(errorLog, latestLog)
        )
    }

    private fun truncateLogContent(content: String): String {
        if (content.length <= MAX_LOG_CONTENT_CHARS) {
            return content
        }
        return content.take(MAX_LOG_CONTENT_CHARS) +
                "\n...(日志内容过长，已截断，仅保留前 $MAX_LOG_CONTENT_CHARS 个字符)"
    }

    private fun containsDownloadHint(content: String, queryMessage: String?): Boolean {
        return queryMessage?.contains(LOG_DOWNLOAD_HINT, ignoreCase = true) == true ||
            content.contains(LOG_DOWNLOAD_HINT, ignoreCase = true)
    }

    private fun parseLogType(logType: String?): LogType? {
        if (logType.isNullOrBlank()) {
            return null
        }
        return try {
            LogType.valueOf(logType.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun buildTextLogResult(
        buildId: String,
        tag: String?,
        jobId: String?,
        stepId: String?,
        logType: String?,
        logsText: QueryLogsText,
        notices: List<String>
    ): String {
        val content = truncateLogContent(logsText.content)
        val lineRange = buildLineRange(logsText)
        val result = linkedMapOf<String, Any?>(
            "buildId" to buildId,
            "tag" to tag,
            "jobId" to jobId,
            "stepId" to stepId,
            "logType" to logType,
            "finished" to logsText.finished,
            "hasMore" to (logsText.hasMore == true),
            "fetchedPages" to 1,
            "fetchedLineCount" to countContentLines(logsText.content),
            "lineRange" to lineRange,
            "notices" to notices,
            "nextActions" to buildMiddleLogNextActions(lineRange),
            "queryMessage" to logsText.message,
            "timeUsed" to logsText.timeUsed,
            "status" to logsText.status,
            "content" to content
        )
        return JsonUtil.toJson(result)
    }

    private fun buildLatestLogNotices(
        logsText: QueryLogsText,
        tag: String?,
        content: String
    ): List<String> = buildList {
        add("已通过日志服务 latest 接口返回当前条件下最新日志窗口，适合 AI 优先分析构建报错。")
        if (tag.isNullOrBlank()) {
            add("未指定 tag，返回的可能是较大范围日志。建议先用构建详情定位失败插件的 elementId 后重试。")
        }
        if (logsText.hasMore == true || logsText.startLineNo > 1L) {
            add("如果最新窗口不足以判断根因，请调用「获取指定行号范围构建日志」按 lineRange 继续向前滚动。")
        }
        if (containsDownloadHint(content, logsText.message)) {
            add("日志已触发熔断，请到蓝盾页面下载完整日志查看。")
        }
    }

    private fun buildMiddleLogNotices(
        logsText: QueryLogsText,
        tag: String?,
        content: String
    ): List<String> = buildList {
        add("已通过日志服务 middle 接口返回指定行号范围日志。")
        if (tag.isNullOrBlank()) {
            add("未指定 tag，返回的可能是较大范围日志。建议优先指定失败插件 elementId。")
        }
        if (logsText.hasMore == true) {
            add("该范围外仍可能存在更多日志，可继续调整 start/end 滚动拉取。")
        }
        if (containsDownloadHint(content, logsText.message)) {
            add("日志已触发熔断，请到蓝盾页面下载完整日志查看。")
        }
    }

    private fun buildMiddleLogNextActions(vararg logMaps: Map<String, Any?>): List<String> {
        val ranges = logMaps.mapNotNull { it["lineRange"] as? String }.filter { it.isNotBlank() }
        val primaryRange = ranges.firstOrNull()
        return buildMiddleLogNextActions(primaryRange)
    }

    private fun buildMiddleLogNextActions(lineRange: String?): List<String> {
        val range = lineRange?.takeIf { it.isNotBlank() } ?: return listOf(
            "如果 latest 日志不足以判断根因，请先扩大 size 或结合失败插件 elementId 调用 middle 范围日志工具。"
        )
        val startLine = range.substringBefore("-").toLongOrNull()
        val beforeStart = startLine?.minus(MIDDLE_LOG_SCROLL_SIZE)?.coerceAtLeast(1L)
        return if (startLine != null && beforeStart != null && beforeStart < startLine) {
            listOf(
                "如果 latest 日志不足以判断根因，请调用「获取指定行号范围构建日志」继续向前滚动。",
                "建议窗口：start=$beforeStart, end=${startLine - 1}，并保持相同 tag/jobId/logType 条件。"
            )
        } else {
            listOf("如果 latest 日志不足以判断根因，请围绕可疑行号调用 middle 范围日志工具扩展上下文。")
        }
    }

    private fun buildLineRange(logsText: QueryLogsText): String? {
        if (logsText.startLineNo <= 0L || logsText.endLineNo <= 0L) {
            return null
        }
        return "${logsText.startLineNo}-${logsText.endLineNo}"
    }

    private fun countContentLines(content: String): Int {
        if (content.isBlank()) {
            return 0
        }
        return content.lineSequence().count()
    }

    private fun buildPipelineDetailUrl(projectId: String, pipelineId: String): String {
        return "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/history"
    }

    private fun buildBuildDetailUrl(projectId: String, pipelineId: String, buildId: String): String {
        return "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val MAX_PAGE_SIZE = 50
        private const val MAX_FAILED_ELEMENTS_TO_ANALYZE = 3
        private const val DEFAULT_LOG_SIZE = 500
        private const val FAILURE_LOG_SIZE = 500
        private const val MAX_LOG_LINES = 10_000
        private const val MAX_LOG_CONTENT_CHARS = 20_000
        private const val MIDDLE_LOG_SCROLL_SIZE = 500L
        private const val LOG_DOWNLOAD_HINT = "Please download logs to view."
    }
}

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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.LogLine
import com.tencent.devops.common.log.pojo.enums.LogType
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
@Suppress("TooManyFunctions", "ComplexCondition")
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
        description = "获取构建日志。强烈建议传入 tag 参数（即 elementId，格式 e-xxxxxxxx）定位到具体插件，" +
                "避免返回全量日志导致内容过大。" +
                "工具会自动继续拉取后续日志片段，但返回文本最多约 20000 字符（超出会截断）。" +
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
        @ToolParam(
            name = "logType",
            description = "日志级别过滤（可选），支持 WARN/ERROR/DEBUG/LOG",
            required = false
        )
        logType: String? = null,
        @ToolParam(name = "jobId", description = "对应 jobId（可选）", required = false)
        jobId: String? = null
    ): String {
        return safeQuery("BuildArtifactTool", "getBuildLogs") {
            val actualLogType = if (logType.isNullOrBlank()) {
                null
            } else {
                parseLogType(logType)
                    ?: return@safeQuery "logType 无效，支持的值为 WARN、ERROR、DEBUG、LOG"
            }
            val debug = actualLogType == LogType.DEBUG
            val result = logResource().getInitLogs(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                debug = debug,
                logType = actualLogType,
                tag = tag,
                containerHashId = null,
                executeCount = null,
                jobId = jobId,
                stepId = stepId
            )
            val initLogs = result.data ?: return@safeQuery "获取日志失败"
            val mergedLogs = initLogs.logs.toMutableList()
            var hasMore = initLogs.hasMore == true
            var finished = initLogs.finished
            var queryMessage = initLogs.message
            var fetchTimes = 0
            var truncatedByLineCap = false

            while (
                hasMore && mergedLogs.size < MAX_MERGED_LOG_LINES &&
                fetchTimes < MAX_LOG_FETCH_TIMES &&
                estimatedRenderedCharCount(mergedLogs) < MAX_LOG_CONTENT_CHARS
            ) {
                val lastLineNo = mergedLogs.lastOrNull()?.lineNo ?: break
                val moreResult = logResource().getAfterLogs(
                    userId = getOperatorUserId(),
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    start = lastLineNo,
                    debug = debug,
                    logType = actualLogType,
                    tag = tag,
                    containerHashId = null,
                    executeCount = null,
                    jobId = jobId,
                    stepId = stepId
                )
                val moreLogs = moreResult.data ?: break
                val newLogs = moreLogs.logs.filter { it.lineNo > lastLineNo }
                if (newLogs.isEmpty()) {
                    hasMore = false
                    break
                }
                val remainingCapacity = (MAX_MERGED_LOG_LINES - mergedLogs.size).coerceAtLeast(0)
                val acceptedLogs = newLogs.take(remainingCapacity)
                truncatedByLineCap = truncatedByLineCap || newLogs.size > acceptedLogs.size
                appendLogs(mergedLogs, acceptedLogs)
                hasMore = moreLogs.hasMore == true
                finished = moreLogs.finished
                if (!moreLogs.message.isNullOrBlank()) {
                    queryMessage = moreLogs.message
                }
                fetchTimes++
            }

            if (mergedLogs.isEmpty()) {
                return@safeQuery buildLogResult(
                    buildId = buildId,
                    tag = tag,
                    jobId = jobId,
                    stepId = stepId,
                    logType = actualLogType?.name,
                    finished = finished,
                    hasMore = hasMore,
                    fetchedLineCount = 0,
                    fetchedPages = fetchTimes + 1,
                    content = "",
                    notices = listOf("未查询到日志内容，请检查 tag、jobId、stepId 是否正确。"),
                    lineRange = null,
                    queryMessage = queryMessage
                )
            }

            val logsForRender = takeLogsForCharBudget(mergedLogs, MAX_LOG_CONTENT_CHARS)
            val renderedContent = renderLogs(logsForRender)
            val truncatedByChars = logsForRender.size < mergedLogs.size
            val contentTruncated = estimatedRenderedCharCount(logsForRender) > MAX_LOG_CONTENT_CHARS
            val hitFetchCap = fetchTimes >= MAX_LOG_FETCH_TIMES && hasMore
            val effectiveHasMore =
                hasMore || truncatedByChars || truncatedByLineCap || contentTruncated || hitFetchCap
            val notices = buildList {
                if (tag.isNullOrBlank()) {
                    add("未指定 tag，返回的可能是较大范围日志。建议先用构建详情定位失败插件的 elementId 后重试。")
                }
                if (truncatedByLineCap) {
                    add("日志行数过多，为避免内存压力，当前最多合并前 $MAX_MERGED_LOG_LINES 行再继续截断。")
                }
                if (contentTruncated) {
                    add("日志文本过长，content 已按最多 $MAX_LOG_CONTENT_CHARS 字符截断用于分析。")
                }
                if (effectiveHasMore) {
                    add("日志仍有后续内容未拉取完，如需更多上下文请继续缩小范围或到页面查看完整日志。")
                }
                if (containsDownloadHint(mergedLogs, queryMessage)) {
                    add("日志已触发熔断，请到蓝盾页面下载完整日志查看。")
                }
            }

            buildLogResult(
                buildId = buildId,
                tag = tag,
                jobId = jobId,
                stepId = stepId,
                logType = actualLogType?.name,
                finished = finished,
                hasMore = effectiveHasMore,
                fetchedLineCount = logsForRender.size,
                fetchedPages = fetchTimes + 1,
                content = renderedContent,
                notices = notices,
                lineRange = logsForRender.firstOrNull()?.let { firstLog ->
                    "${firstLog.lineNo}-${logsForRender.last().lineNo}"
                },
                queryMessage = queryMessage
            )
        }
    }

    private fun appendLogs(target: MutableList<LogLine>, logs: List<LogLine>) {
        logs.forEach { log ->
            if (target.size >= MAX_MERGED_LOG_LINES) {
                return
            }
            if (target.none { it.lineNo == log.lineNo }) {
                target.add(log)
            }
        }
    }

    private fun renderLogs(logs: List<LogLine>): String {
        return logs.joinToString(separator = "\n") { log ->
            "[${log.lineNo}] ${log.message}"
        }.let(::truncateLogContent)
    }

    private fun truncateLogContent(content: String): String {
        if (content.length <= MAX_LOG_CONTENT_CHARS) {
            return content
        }
        return content.take(MAX_LOG_CONTENT_CHARS) +
                "\n...(日志内容过长，已截断，仅保留前 $MAX_LOG_CONTENT_CHARS 个字符)"
    }

    private fun estimatedRenderedCharCount(logs: List<LogLine>): Int {
        if (logs.isEmpty()) {
            return 0
        }
        var total = 0
        logs.forEachIndexed { index, log ->
            total += LOG_LINE_PREFIX_OVERHEAD + log.lineNo.toString().length + log.message.length
            if (index > 0) {
                total += 1
            }
        }
        return total
    }

    private fun takeLogsForCharBudget(logs: List<LogLine>, maxChars: Int): List<LogLine> {
        if (logs.isEmpty()) {
            return emptyList()
        }
        val selected = ArrayList<LogLine>(minOf(logs.size, 256))
        var total = 0
        logs.forEachIndexed { index, log ->
            val lineChars = LOG_LINE_PREFIX_OVERHEAD + log.lineNo.toString().length + log.message.length
            val addChars = if (index == 0) lineChars else lineChars + 1
            if (total + addChars > maxChars) {
                if (selected.isEmpty()) {
                    selected.add(log)
                }
                return selected
            }
            selected.add(log)
            total += addChars
        }
        return selected
    }

    private fun containsDownloadHint(logs: List<LogLine>, queryMessage: String?): Boolean {
        if (queryMessage?.contains(LOG_DOWNLOAD_HINT, ignoreCase = true) == true) {
            return true
        }
        return logs.any { it.message.contains(LOG_DOWNLOAD_HINT, ignoreCase = true) }
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

    private fun buildLogResult(
        buildId: String,
        tag: String?,
        jobId: String?,
        stepId: String?,
        logType: String?,
        finished: Boolean,
        hasMore: Boolean,
        fetchedLineCount: Int,
        fetchedPages: Int,
        content: String,
        notices: List<String>,
        lineRange: String?,
        queryMessage: String?
    ): String {
        val result = linkedMapOf<String, Any?>(
            "buildId" to buildId,
            "tag" to tag,
            "jobId" to jobId,
            "stepId" to stepId,
            "logType" to logType,
            "finished" to finished,
            "hasMore" to hasMore,
            "fetchedPages" to fetchedPages,
            "fetchedLineCount" to fetchedLineCount,
            "lineRange" to lineRange,
            "notices" to notices,
            "queryMessage" to queryMessage,
            "content" to content
        )
        return JsonUtil.toJson(result)
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val MAX_PAGE_SIZE = 50
        private const val MAX_LOG_FETCH_TIMES = 5
        private const val MAX_MERGED_LOG_LINES = 10_000
        private const val MAX_LOG_CONTENT_CHARS = 20_000
        private const val LOG_LINE_PREFIX_OVERHEAD = 3
        private const val LOG_DOWNLOAD_HINT = "Please download logs to view."
    }
}

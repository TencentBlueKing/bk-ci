/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.log.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.LogPanelLine
import com.tencent.devops.common.log.pojo.QueryLogPanel
import com.tencent.devops.common.log.pojo.enums.LogPanelLevel
import com.tencent.devops.common.log.pojo.enums.LogStatus
import com.tencent.devops.log.dao.LogPanelEsDao
import com.tencent.devops.log.util.LogPanelQueryBuilder
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class LogPanelQueryService(
    private val buildLogQueryService: BuildLogQueryService,
    private val logPanelEsDao: ObjectProvider<LogPanelEsDao>,
    private val logService: LogService
) {
    fun latest(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        levels: String?,
        pageSize: Int?,
        archiveFlag: Boolean?
    ): Result<QueryLogPanel> {
        authorize(userId, projectId, pipelineId, buildId, archiveFlag)
        return Result(
            query(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                levels = levels,
                pageSize = pageSize,
                direction = LogPanelEsDao.Direction.LATEST,
                cursorLineNo = null
            )
        )
    }

    fun before(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        endLineNo: Long,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        levels: String?,
        pageSize: Int?,
        archiveFlag: Boolean?
    ): Result<QueryLogPanel> {
        authorize(userId, projectId, pipelineId, buildId, archiveFlag)
        return Result(
            query(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                levels = levels,
                pageSize = pageSize,
                direction = LogPanelEsDao.Direction.BEFORE,
                cursorLineNo = endLineNo
            )
        )
    }

    fun after(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        startLineNo: Long,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        levels: String?,
        pageSize: Int?,
        archiveFlag: Boolean?
    ): Result<QueryLogPanel> {
        authorize(userId, projectId, pipelineId, buildId, archiveFlag)
        return Result(
            query(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                levels = levels,
                pageSize = pageSize,
                direction = LogPanelEsDao.Direction.AFTER,
                cursorLineNo = startLineNo
            )
        )
    }

    private fun authorize(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        archiveFlag: Boolean?
    ) {
        buildLogQueryService.checkViewPermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            archiveFlag = archiveFlag
        )
    }

    private fun query(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        levels: String?,
        pageSize: Int?,
        direction: LogPanelEsDao.Direction,
        cursorLineNo: Long?
    ): QueryLogPanel {
        val parsedLevels = LogPanelLevel.parseList(levels)
        val size = LogPanelQueryBuilder.normalizePageSize(pageSize)
        val dao = logPanelEsDao.getIfAvailable()
        return if (dao != null) {
            dao.query(
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                containerHashId = jobId,
                executeCount = executeCount,
                levels = parsedLevels,
                pageSize = size,
                direction = direction,
                cursorLineNo = cursorLineNo
            )
        } else {
            fallback(buildId, tag, subTag, jobId, executeCount, parsedLevels, size, direction, cursorLineNo)
        }
    }

    /**
     * Lucene / 无 ES 客户端时走存量 LogService，不改写路径。
     * 多级别无法精确表达时退化为 debug=false（INFO/WARN/ERROR）。
     */
    private fun fallback(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        levels: List<LogPanelLevel>,
        pageSize: Int,
        direction: LogPanelEsDao.Direction,
        cursorLineNo: Long?
    ): QueryLogPanel {
        val debug = LogPanelLevel.DEBUG in levels
        val singleType = levels.singleOrNull()?.toEsLogType()
        val result = when (direction) {
            LogPanelEsDao.Direction.LATEST -> logService.getBottomLogs(
                pipelineId = "",
                buildId = buildId,
                debug = debug,
                logType = singleType,
                tag = tag,
                subTag = subTag,
                containerHashId = jobId,
                executeCount = executeCount,
                size = pageSize,
                jobId = null,
                stepId = null
            )
            LogPanelEsDao.Direction.BEFORE -> logService.queryLogsBeforeLine(
                buildId = buildId,
                end = (cursorLineNo ?: 1L) - 1,
                debug = debug,
                logType = singleType,
                size = pageSize,
                tag = tag,
                subTag = subTag,
                containerHashId = jobId,
                executeCount = executeCount,
                jobId = null,
                stepId = null
            )
            LogPanelEsDao.Direction.AFTER -> logService.queryLogsAfterLine(
                buildId = buildId,
                start = (cursorLineNo ?: 0L) + 1,
                debug = debug,
                logType = singleType,
                tag = tag,
                subTag = subTag,
                containerHashId = jobId,
                executeCount = executeCount,
                jobId = null,
                stepId = null
            )
        }
        val cleaned = result.status == LogStatus.CLEAN.status || result.status == LogStatus.CLOSED.status
        val lines = result.logs.takeLast(pageSize).map { line ->
            LogPanelLine(
                lineNo = line.lineNo,
                timestamp = line.timestamp,
                message = line.message,
                level = LogPanelLevel.INFO.name,
                tag = line.tag,
                subTag = line.subTag,
                executeCount = line.executeCount
            )
        }
        val more = result.hasMore == true
        return QueryLogPanel(
            buildId = buildId,
            finished = result.finished,
            cleaned = cleaned,
            status = result.status,
            message = LogPanelQueryBuilder.cleanedMessage(result.status) ?: result.message,
            subTags = result.subTags,
            logs = lines,
            startLineNo = lines.firstOrNull()?.lineNo,
            endLineNo = lines.lastOrNull()?.lineNo,
            hasBefore = direction != LogPanelEsDao.Direction.AFTER && more,
            hasAfter = direction != LogPanelEsDao.Direction.BEFORE && (more || !result.finished),
            levels = levels.map { it.name }
        )
    }
}

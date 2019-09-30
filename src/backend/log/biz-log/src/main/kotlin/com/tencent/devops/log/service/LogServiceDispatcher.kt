package com.tencent.devops.log.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.log.model.pojo.EndPageQueryLogs
import com.tencent.devops.log.model.pojo.LogBatchEvent
import com.tencent.devops.log.model.pojo.LogEvent
import com.tencent.devops.log.model.pojo.LogStatusEvent
import com.tencent.devops.log.model.pojo.PageQueryLogs
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.log.service.v2.LogServiceV2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class LogServiceDispatcher @Autowired constructor(
    private val logService: PipelineLogService,
    private val indexService: IndexService,
    private val logServiceV2: LogServiceV2,
    private val v2ProjectService: V2ProjectService
) {

    fun getInitLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        if (v2ProjectService.buildEnable(buildId, projectId)) {
            return Result(
                logServiceV2.queryInitLogs(
                    buildId,
                    isAnalysis ?: false,
                    queryKeywords,
                    tag,
                    executeCount
                )
            )
        } else {
            val indexAndType = indexService.parseIndexAndType(buildId)
            return Result(
                logService.queryInitLogs(
                    buildId,
                    indexAndType.left,
                    indexAndType.right,
                    isAnalysis ?: false,
                    queryKeywords,
                    tag,
                    executeCount
                )
            )
        }
    }

    fun getInitLogsPage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?,
        page: Int?,
        pageSize: Int?
    ): Result<PageQueryLogs> {
        if (v2ProjectService.buildEnable(buildId, projectId)) {
            return Result(
                logServiceV2.queryInitLogsPage(
                    buildId,
                    isAnalysis ?: false,
                    queryKeywords,
                    tag,
                    executeCount,
                    page ?: -1,
                    pageSize ?: -1
                )
            )
        } else {
            val indexAndType = indexService.parseIndexAndType(buildId)
            return Result(
                logService.queryInitLogsPage(
                    buildId,
                    indexAndType.left,
                    indexAndType.right,
                    isAnalysis ?: false,
                    queryKeywords,
                    tag,
                    executeCount,
                    page ?: -1,
                    pageSize ?: -1
                )
            )
        }
    }

    fun getMoreLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        if (v2ProjectService.buildEnable(buildId, projectId)) {
            return Result(
                logServiceV2.queryMoreLogsBetweenLines(
                    buildId,
                    num ?: 100,
                    fromStart ?: true,
                    start,
                    end,
                    tag,
                    executeCount
                )
            )
        } else {
            val indexAndType = indexService.parseIndexAndType(buildId)

            return Result(
                logService.queryMoreLogsBetweenLines(
                    buildId,
                    indexAndType.left,
                    indexAndType.right,
                    num ?: 100,
                    fromStart ?: true,
                    start,
                    end,
                    tag,
                    executeCount
                )
            )
        }
    }

    fun getAfterLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        if (v2ProjectService.buildEnable(buildId, projectId)) {
            return Result(
                logServiceV2.queryMoreLogsAfterLine(
                    buildId,
                    start,
                    isAnalysis ?: false,
                    queryKeywords,
                    tag,
                    executeCount
                )
            )
        } else {
            val indexAndType = indexService.parseIndexAndType(buildId)

            return Result(
                logService.queryMoreLogsAfterLine(
                    buildId,
                    indexAndType.left,
                    indexAndType.right,
                    start,
                    isAnalysis ?: false,
                    queryKeywords,
                    tag,
                    executeCount
                )
            )
        }
    }

    fun downloadLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        executeCount: Int?
    ): Response {
        return if (v2ProjectService.buildEnable(buildId, projectId)) {
            logServiceV2.downloadLogs(pipelineId, buildId, tag, executeCount)
        } else {
            logService.downloadLogs(pipelineId, buildId, tag ?: "", executeCount)
        }
    }

    fun getEndLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        size: Int,
        tag: String?,
        executeCount: Int?
    ): Result<EndPageQueryLogs> {
        return if (v2ProjectService.buildEnable(buildId, projectId)) {
            Result(logServiceV2.getEndLogs(pipelineId, buildId, tag, executeCount, size))
        } else {
            Result(logService.getEndLogs(pipelineId, buildId, tag ?: "", executeCount, size))
        }
    }

    fun logEvent(event: LogEvent) {
        if (v2ProjectService.buildEnable(event.buildId)) {
            logServiceV2.addLogEvent(event)
        } else {
            logService.addLogEvent(event)
        }
    }

    fun logBatchEvent(event: LogBatchEvent) {
        if (v2ProjectService.buildEnable(event.buildId)) {
            logServiceV2.addBatchLogEvent(event)
        } else {
            logService.addBatchLogEvent(event)
        }
    }

    fun logStatusEvent(event: LogStatusEvent) {
        if (v2ProjectService.buildEnable(event.buildId)) {
            logServiceV2.updateLogStatus(event)
        } else {
            logService.upsertLogStatus(event)
        }
    }
}
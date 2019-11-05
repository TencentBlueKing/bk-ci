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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        if (v2ProjectService.buildEnable(buildId, projectId)) {
            return Result(
                logServiceV2.queryInitLogs(
                    buildId,
                    isAnalysis ?: false,
                    queryKeywords,
                    tag,
                    jobId,
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
        jobId: String?,
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
                    jobId,
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
        jobId: String?,
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
                    jobId,
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
        jobId: String?,
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
                    jobId,
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
        jobId: String?,
        executeCount: Int?
    ): Response {
        return if (v2ProjectService.buildEnable(buildId, projectId)) {
            logServiceV2.downloadLogs(pipelineId, buildId, tag, jobId, executeCount)
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
        jobId: String?,
        executeCount: Int?
    ): Result<EndPageQueryLogs> {
        return if (v2ProjectService.buildEnable(buildId, projectId)) {
            Result(logServiceV2.getEndLogs(pipelineId, buildId, tag, jobId, executeCount, size))
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
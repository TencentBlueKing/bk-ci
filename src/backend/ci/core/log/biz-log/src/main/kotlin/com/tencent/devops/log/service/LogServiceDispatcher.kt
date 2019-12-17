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
    private val logServiceV2: LogServiceV2
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
            return Result(
                logServiceV2.queryMoreOriginLogsAfterLine(
                    buildId = buildId,
                    start = start,
                    tag = tag,
                    jobId = jobId,
                    executeCount = executeCount
                )
            )
    }

    fun downloadLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Response {
        return logServiceV2.downloadLogs(pipelineId, buildId, tag, jobId, executeCount)
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
        return Result(logServiceV2.getEndLogs(pipelineId, buildId, tag, jobId, executeCount, size))
    }

    fun logEvent(event: LogEvent) {
        logServiceV2.addLogEvent(event)
    }

    fun logBatchEvent(event: LogBatchEvent) {
        logServiceV2.addBatchLogEvent(event)
    }

    fun logStatusEvent(event: LogStatusEvent) {
        logServiceV2.updateLogStatus(event)
    }
}
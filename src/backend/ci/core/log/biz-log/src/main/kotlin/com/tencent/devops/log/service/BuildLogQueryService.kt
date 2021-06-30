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

package com.tencent.devops.log.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.EndPageQueryLogs
import com.tencent.devops.common.log.pojo.PageQueryLogs
import com.tencent.devops.common.log.pojo.QueryLogStatus
import com.tencent.devops.common.log.pojo.QueryLogs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class BuildLogQueryService @Autowired constructor(
    private val logService: LogService,
    private val logStatusService: LogStatusService
) {

    fun getInitLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        return Result(
            logService.queryInitLogs(
                buildId = buildId,
                debug = debug ?: false,
                subTag = subTag,
                tag = tag,
                jobId = jobId,
                executeCount = executeCount
            )
        )
    }

    fun getInitLogsPage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        page: Int?,
        pageSize: Int?,
        subTag: String? = null
    ): Result<PageQueryLogs> {
        return Result(
            logService.queryInitLogsPage(
                buildId = buildId,
                debug = debug ?: false,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                page = page ?: -1,
                pageSize = pageSize ?: -1
            )
        )
    }

    fun getMoreLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        return Result(
            logService.queryLogsBetweenLines(
                buildId = buildId,
                num = num ?: 100,
                fromStart = fromStart ?: true,
                start = start,
                end = end,
                debug = debug ?: false,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
        )
    }

    fun getAfterLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        debug: Boolean?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        return Result(
            logService.queryLogsAfterLine(
                buildId = buildId,
                start = start,
                debug = debug ?: false,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
        )
    }

    fun getBeforeLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        end: Long,
        debug: Boolean?,
        size: Int?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        return Result(
            logService.queryLogsBeforeLine(
                buildId = buildId,
                end = end,
                size = size,
                debug = debug ?: false,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
        )
    }

    fun getLogMode(
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String,
        executeCount: Int?
    ): Result<QueryLogStatus> {
        return Result(
            logStatusService.getStorageMode(
                buildId = buildId,
                tag = tag,
                executeCount = executeCount ?: 1
            )
        )
    }

    fun downloadLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        fileName: String?,
        subTag: String? = null
    ): Response {
        return logService.downloadLogs(
            pipelineId = pipelineId,
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount,
            fileName = fileName
        )
    }

    fun getEndLogsPage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        size: Int,
        debug: Boolean?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<EndPageQueryLogs> {
        return Result(logService.getEndLogsPage(
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug ?: false,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount,
            size = size
        ))
    }

    fun getBottomLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        size: Int?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        return Result(logService.getBottomLogs(
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug ?: false,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount,
            size = size
        ))
    }
}

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

package com.tencent.devops.log.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogLineNum
import com.tencent.devops.common.log.pojo.QueryLogStatus
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.log.service.BuildLogQueryService
import org.springframework.beans.factory.annotation.Autowired
import jakarta.ws.rs.core.Response

/**
 *
 * Powered By Tencent
 */
@RestResource
class ServiceLogResourceImpl @Autowired constructor(
    private val buildLogQueryService: BuildLogQueryService
) : ServiceLogResource {

    companion object {
        private const val defaultNum = 100
    }

    override fun getInitLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        logType: LogType?,
        tag: String?,
        containerHashId: String?,
        executeCount: Int?,
        subTag: String?,
        jobId: String?,
        stepId: String?,
        archiveFlag: Boolean?,
        checkPermissionFlag: Boolean,
        reverse: Boolean?
    ): Result<QueryLogs> {
        return buildLogQueryService.getInitLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug,
            logType = logType,
            tag = tag,
            containerHashId = containerHashId,
            executeCount = executeCount,
            subTag = subTag,
            jobId = jobId,
            stepId = stepId,
            archiveFlag = archiveFlag,
            reverse = reverse ?: false,
            checkPermissionFlag = checkPermissionFlag
        )
    }

    override fun getMoreLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        logType: LogType?,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        containerHashId: String?,
        executeCount: Int?,
        jobId: String?,
        stepId: String?,
        archiveFlag: Boolean?,
        checkPermissionFlag: Boolean
    ): Result<QueryLogs> {
        return buildLogQueryService.getMoreLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug,
            logType = logType,
            num = num ?: defaultNum,
            fromStart = fromStart,
            start = start,
            end = end,
            tag = tag,
            containerHashId = containerHashId,
            executeCount = executeCount,
            jobId = jobId,
            stepId = stepId,
            archiveFlag = archiveFlag,
            checkPermissionFlag = checkPermissionFlag
        )
    }

    override fun getAfterLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        debug: Boolean?,
        logType: LogType?,
        tag: String?,
        containerHashId: String?,
        executeCount: Int?,
        jobId: String?,
        stepId: String?,
        archiveFlag: Boolean?,
        checkPermissionFlag: Boolean
    ): Result<QueryLogs> {
        return buildLogQueryService.getAfterLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            start = start,
            debug = debug,
            logType = logType,
            tag = tag,
            containerHashId = containerHashId,
            executeCount = executeCount,
            jobId = jobId,
            stepId = stepId,
            archiveFlag = archiveFlag,
            checkPermissionFlag = checkPermissionFlag
        )
    }

    override fun downloadLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        containerHashId: String?,
        executeCount: Int?,
        jobId: String?,
        stepId: String?,
        archiveFlag: Boolean?
    ): Response {
        return buildLogQueryService.downloadLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            tag = tag,
            containerHashId = containerHashId,
            executeCount = executeCount,
            fileName = null,
            jobId = jobId,
            stepId = stepId,
            archiveFlag = archiveFlag
        )
    }

    override fun getLogMode(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        executeCount: Int?,
        stepId: String?,
        archiveFlag: Boolean?
    ): Result<QueryLogStatus> {
        return buildLogQueryService.getLogMode(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            tag = tag,
            executeCount = executeCount,
            stepId = stepId,
            archiveFlag = archiveFlag
        )
    }

    override fun getLogLastLineNum(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        archiveFlag: Boolean?
    ): Result<QueryLogLineNum> {
        return buildLogQueryService.getLastLineNum(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            archiveFlag = archiveFlag
        )
    }
}

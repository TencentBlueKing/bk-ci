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

package com.tencent.devops.ai.resources

import com.tencent.devops.ai.agent.build.BuildTools
import com.tencent.devops.ai.api.op.OpAiBuildAgentToolsResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

/**
 * 运营联调：封装构建 Agent 的 BuildTools，与 Agent 内调用链一致（Client + 操作人 userId）。
 */
@RestResource
class OpAiBuildAgentToolsResourceImpl @Autowired constructor(
    private val client: Client
) : OpAiBuildAgentToolsResource {

    private fun buildTools(userId: String) = BuildTools(client) { userId }

    override fun searchPipelines(
        userId: String,
        projectId: String,
        keyword: String?,
        page: Int?,
        pageSize: Int?
    ): Result<String> {
        return Result(buildTools(userId).searchPipelines(projectId, keyword, page, pageSize))
    }

    override fun getPipelineInfo(userId: String, projectId: String, pipelineId: String): Result<String> {
        return Result(buildTools(userId).getPipelineInfo(projectId, pipelineId))
    }

    override fun getPipelineStatus(userId: String, projectId: String, pipelineId: String): Result<String> {
        return Result(buildTools(userId).getPipelineStatus(projectId, pipelineId))
    }

    override fun getPipelineModel(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?
    ): Result<String> {
        return Result(buildTools(userId).getPipelineModel(projectId, pipelineId, version))
    }

    override fun getPipelineModelSummary(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?,
        includeElements: Boolean?
    ): Result<String> {
        return Result(buildTools(userId).getPipelineModelSummary(projectId, pipelineId, version, includeElements))
    }

    override fun getPipelineNodeDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?,
        stageId: String?,
        containerHashId: String?,
        containerId: String?,
        jobId: String?,
        elementId: String?,
        stepId: String?
    ): Result<String> {
        return Result(
            buildTools(userId).getPipelineNodeDetail(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                stageId = stageId,
                containerHashId = containerHashId,
                containerId = containerId,
                jobId = jobId,
                elementId = elementId,
                stepId = stepId
            )
        )
    }

    override fun getManualStartupInfo(userId: String, projectId: String, pipelineId: String): Result<String> {
        return Result(buildTools(userId).getManualStartupInfo(projectId, pipelineId))
    }

    override fun triggerBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        params: String?
    ): Result<String> {
        return Result(buildTools(userId).triggerBuild(projectId, pipelineId, params))
    }

    override fun retryBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<String> {
        return Result(buildTools(userId).retryBuild(projectId, pipelineId, buildId))
    }

    override fun stopBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<String> {
        return Result(buildTools(userId).stopBuild(projectId, pipelineId, buildId))
    }

    override fun getBuildHistory(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        status: String?,
        startUser: String?
    ): Result<String> {
        return Result(
            buildTools(userId).getBuildHistory(
                projectId = projectId,
                pipelineId = pipelineId,
                page = page,
                pageSize = pageSize,
                status = status,
                startUser = startUser
            )
        )
    }

    override fun getBuildDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<String> {
        return Result(buildTools(userId).getBuildDetail(projectId, pipelineId, buildId))
    }

    override fun analyzeBuildFailure(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?
    ): Result<String> {
        return Result(buildTools(userId).analyzeBuildFailure(projectId, pipelineId, buildId))
    }

    override fun getBuildStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<String> {
        return Result(buildTools(userId).getBuildStatus(projectId, pipelineId, buildId))
    }

    override fun getBuildVars(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<String> {
        return Result(buildTools(userId).getBuildVars(projectId, pipelineId, buildId))
    }

    override fun getBuildLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        stepId: String?,
        logType: String?,
        jobId: String?,
        size: Int?
    ): Result<String> {
        return Result(
            buildTools(userId).getBuildLogs(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                tag = tag,
                stepId = stepId,
                logType = logType,
                jobId = jobId,
                size = size
            )
        )
    }

    override fun getMiddleBuildLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        end: Long,
        tag: String?,
        stepId: String?,
        logType: String?,
        jobId: String?
    ): Result<String> {
        return Result(
            buildTools(userId).getMiddleBuildLogs(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                start = start,
                end = end,
                tag = tag,
                stepId = stepId,
                logType = logType,
                jobId = jobId
            )
        )
    }
}

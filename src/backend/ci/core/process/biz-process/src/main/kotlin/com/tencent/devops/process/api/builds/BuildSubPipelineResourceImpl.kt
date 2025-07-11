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

package com.tencent.devops.process.api.builds

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.pipeline.ProjectBuildId
import com.tencent.devops.process.pojo.pipeline.PipelineBuildParamFormProp
import com.tencent.devops.process.pojo.pipeline.SubPipelineStatus
import com.tencent.devops.process.service.SubPipelineStartUpService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildSubPipelineResourceImpl @Autowired constructor(
    private val subPipeService: SubPipelineStartUpService
) : BuildSubPipelineResource {

    @AuditEntry(actionId = ActionId.PIPELINE_EXECUTE)
    override fun callOtherProjectPipelineStartup(
        projectId: String,
        parentPipelineId: String,
        buildId: String,
        callProjectId: String,
        callPipelineId: String,
        atomCode: String,
        taskId: String,
        runMode: String,
        values: Map<String, String>,
        executeCount: Int?,
        branch: String?
    ): Result<ProjectBuildId> {
        return subPipeService.callPipelineStartup(
            projectId = projectId,
            parentPipelineId = parentPipelineId,
            buildId = buildId,
            callProjectId = callProjectId,
            callPipelineId = callPipelineId,
            atomCode = atomCode,
            taskId = taskId,
            runMode = runMode,
            values = values,
            executeCount = executeCount,
            branch = branch
        )
    }

    override fun callPipelineStartup(
        projectId: String,
        parentPipelineId: String,
        buildId: String,
        callPipelineId: String,
        atomCode: String,
        taskId: String,
        runMode: String,
        channelCode: ChannelCode?,
        values: Map<String, String>,
        executeCount: Int?,
        branch: String?
    ): Result<ProjectBuildId> {
        return subPipeService.callPipelineStartup(
            projectId = projectId,
            parentPipelineId = parentPipelineId,
            buildId = buildId,
            callPipelineId = callPipelineId,
            atomCode = atomCode,
            taskId = taskId,
            runMode = runMode,
            channelCode = channelCode,
            values = values,
            executeCount = executeCount,
            branch = branch
        )
    }

    override fun getSubPipelineStatus(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<SubPipelineStatus> {
        return subPipeService.getSubPipelineStatus(projectId, buildId)
    }

    override fun subpipManualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        includeConst: Boolean?,
        includeNotRequired: Boolean?,
        parentProjectId: String,
        parentPipelineId: String,
        branch: String?
    ): Result<List<PipelineBuildParamFormProp>> {
        checkParam(userId)
        return subPipeService.subPipelineManualStartupInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            includeConst = includeConst,
            includeNotRequired = includeNotRequired,
            parentPipelineId = parentPipelineId,
            parentProjectId = parentProjectId,
            branch = branch
        )
    }

    override fun getPipelineByName(projectId: String, pipelineName: String): Result<List<PipelineId?>> {
        return subPipeService.getPipelineByName(projectId, pipelineName)
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}

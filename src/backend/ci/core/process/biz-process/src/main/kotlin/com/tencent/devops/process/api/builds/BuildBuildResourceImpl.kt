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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.vmbuild.EngineVMBuildService
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.task.PipelineFailTaskDetail
import com.tencent.devops.process.service.SubPipelineStartUpService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildBuildResourceImpl @Autowired constructor(
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val subPipelineStartUpService: SubPipelineStartUpService,
    private val vMBuildService: EngineVMBuildService,
    private val pipelineUrlBean: PipelineUrlBean
) : BuildBuildResource {

    @BkApiPermission([BkApiHandleType.BUILD_API_AUTH_CHECK])
    override fun getSingleHistoryBuild(
        projectId: String,
        pipelineId: String,
        buildNum: String,
        buildId: String?
    ): Result<BuildHistory?> {
        return Result(
            data = pipelineBuildFacadeService.getSingleHistoryBuild(
                projectId = projectId,
                pipelineId = pipelineId,
                buildNum = buildNum.toInt(),
                buildId = buildId
            )
        )
    }

    @BkApiPermission([BkApiHandleType.BUILD_API_AUTH_CHECK])
    override fun getLatestSuccessBuild(
        projectId: String,
        pipelineId: String,
        buildId: String?
    ): Result<BuildHistory?> {
        return Result(
            data = pipelineBuildFacadeService.getLatestSuccessBuild(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
        )
    }

    @BkApiPermission([BkApiHandleType.BUILD_API_AUTH_CHECK])
    override fun getBuildDetail(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<ModelDetail> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            data = pipelineBuildFacadeService.getBuildDetail(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
        )
    }

    @BkApiPermission([BkApiHandleType.BUILD_API_AUTH_CHECK])
    override fun getBuildStatus(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<BuildHistory> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        // 按项目+流水线+构建三要素精确查询，避免路径参数不一致时也能查到构建
        pipelineRuntimeService.getBuildInfo(projectId, pipelineId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        val buildHistory = pipelineRuntimeService.getBuildHistoryById(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        return Result(data = buildHistory)
    }

    override fun getSubBuildVars(projectId: String, buildId: String, taskId: String): Result<Map<String, String>> {
        return subPipelineStartUpService.getSubVar(projectId = projectId, buildId = buildId, taskId = taskId)
    }

    override fun getBuildDetailUrl(projectId: String, pipelineId: String, buildId: String): Result<String> {
        return Result(pipelineUrlBean.genBuildDetailUrl(projectId, pipelineId, buildId, null, null, true))
    }

    override fun getBuildDispatchType(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String
    ): Result<String?> {
        val container = vMBuildService.getBuildContainer(projectId, pipelineId, buildId, vmSeqId)
        return Result(container?.dispatchType?.buildType()?.name)
    }

    override fun getBuildFailedTasks(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?
    ): Result<List<PipelineFailTaskDetail>> {
        return Result(
            pipelineBuildFacadeService.getBuildFailedTasks(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = executeCount
            )
        )
    }

    override fun getTaskParams(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Result<Map<String, Any>?> {
        val task = pipelineBuildFacadeService.getByTaskId(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId
        )
        return Result(task?.taskParams ?: mapOf())
    }
}

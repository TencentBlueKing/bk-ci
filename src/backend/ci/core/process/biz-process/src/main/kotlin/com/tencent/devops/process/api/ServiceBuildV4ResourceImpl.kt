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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServiceBuildV4Resource
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.BuildTaskPauseInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.service.builds.PipelinePauseBuildFacadeService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class ServiceBuildV4ResourceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelinePauseBuildFacadeService: PipelinePauseBuildFacadeService
) : ServiceBuildV4Resource {
    override fun manualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String?,
        channelCode: ChannelCode
    ): Result<BuildManualStartupInfo> {
        return Result(
            pipelineBuildFacadeService.buildManualStartupInfo(
                userId = checkUserId(userId),
                projectId = checkProjectId(projectId),
                pipelineId = checkPipelineId(pipelineId),
                channelCode = channelCode,
                checkPermission = ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    override fun getHistoryBuild(
        userId: String,
        projectId: String,
        pipelineId: String?,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode
    ): Result<BuildHistoryPage<BuildHistory>> {
        val result = pipelineBuildFacadeService.getHistoryBuild(
            userId = checkUserId(userId),
            projectId = checkProjectId(projectId),
            pipelineId = checkPipelineId(pipelineId),
            page = page,
            pageSize = pageSize,
            channelCode = channelCode,
            checkPermission = ChannelCode.isNeedAuth(channelCode)
        )
        return Result(result)
    }

    override fun manualStartupNew(
        userId: String,
        projectId: String,
        pipelineId: String?,
        values: Map<String, String>,
        channelCode: ChannelCode,
        buildNo: Int?,
        startType: StartType
    ): Result<BuildId> {
        return Result(
            BuildId(
                pipelineBuildFacadeService.buildManualStartup(
                    userId = checkUserId(userId),
                    startType = startType,
                    projectId = checkProjectId(projectId),
                    pipelineId = checkPipelineId(pipelineId),
                    values = values,
                    channelCode = channelCode,
                    buildNo = buildNo,
                    checkPermission = ChannelCode.isNeedAuth(channelCode),
                    frequencyLimit = true
                )
            )
        )
    }

    override fun retry(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        taskId: String?,
        failedContainer: Boolean?,
        skipFailedTask: Boolean?,
        channelCode: ChannelCode,
        checkManualStartup: Boolean?
    ): Result<BuildId> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val pipelineBuildInfo =
            pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: throw ParamBlankException("Invalid buildId")
        return Result(
            BuildId(
                pipelineBuildFacadeService.retry(
                    userId = checkUserId(userId),
                    projectId = checkProjectId(projectId),
                    pipelineId = checkPipelineId(pipelineId, pipelineBuildInfo.pipelineId),
                    buildId = buildId,
                    taskId = taskId,
                    failedContainer = failedContainer,
                    skipFailedTask = skipFailedTask,
                    isMobile = false,
                    channelCode = channelCode,
                    checkPermission = ChannelCode.isNeedAuth(channelCode),
                    checkManualStartup = checkManualStartup ?: false
                )
            )
        )
    }

    override fun manualShutdown(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        channelCode: ChannelCode
    ): Result<Boolean> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val pipelineBuildInfo =
            pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: throw ParamBlankException("Invalid buildId")
        pipelineBuildFacadeService.buildManualShutdown(
            userId = checkUserId(userId),
            projectId = checkProjectId(projectId),
            pipelineId = checkPipelineId(pipelineId, pipelineBuildInfo.pipelineId),
            buildId = buildId,
            channelCode = channelCode,
            checkPermission = ChannelCode.isNeedAuth(channelCode)
        )
        return Result(true)
    }

    override fun getBuildDetail(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        channelCode: ChannelCode
    ): Result<ModelDetail> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val pipelineBuildInfo =
            pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: throw ParamBlankException("Invalid buildId")
        return Result(
            pipelineBuildFacadeService.getBuildDetail(
                userId = checkUserId(userId),
                projectId = checkProjectId(projectId),
                pipelineId = checkPipelineId(pipelineId, pipelineBuildInfo.pipelineId),
                buildId = buildId,
                channelCode = channelCode,
                checkPermission = ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    override fun getBuildStatus(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        channelCode: ChannelCode
    ): Result<BuildHistoryWithVars> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val pipelineBuildInfo =
            pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: throw ParamBlankException("Invalid buildId")
        return Result(
            pipelineBuildFacadeService.getBuildStatusWithVars(
                userId = checkUserId(userId),
                projectId = checkProjectId(projectId),
                pipelineId = checkPipelineId(pipelineId, pipelineBuildInfo.pipelineId),
                buildId = buildId,
                channelCode = channelCode,
                checkPermission = ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    override fun getBuildVariableValue(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        channelCode: ChannelCode,
        variableNames: List<String>
    ): Result<Map<String, String>> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (variableNames.isEmpty()) {
            throw ParamBlankException("Invalid variableNames")
        }
        if (variableNames.size > 50) {
            throw IllegalArgumentException("The maximum number of variableNames is 50")
        }
        val pipelineBuildInfo =
            pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: throw ParamBlankException("Invalid buildId")
        return Result(
            pipelineBuildFacadeService.getBuildVarsByNames(
                userId = checkUserId(userId),
                projectId = checkProjectId(projectId),
                pipelineId = checkPipelineId(pipelineId, pipelineBuildInfo.pipelineId),
                buildId = buildId,
                variableNames = variableNames,
                checkPermission = ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    override fun manualStartStage(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        stageId: String,
        cancel: Boolean?,
        reviewRequest: StageReviewRequest?
    ): Result<Boolean> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (stageId.isBlank()) {
            throw ParamBlankException("Invalid stageId")
        }
        val pipelineBuildInfo =
            pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: throw ParamBlankException("Invalid buildId")
        pipelineBuildFacadeService.buildManualStartStage(
            userId = checkUserId(userId),
            projectId = checkProjectId(projectId),
            pipelineId = checkPipelineId(pipelineId, pipelineBuildInfo.pipelineId),
            buildId = buildId,
            stageId = stageId,
            isCancel = cancel ?: false,
            reviewRequest = reviewRequest
        )
        return Result(true)
    }

    override fun executionPauseAtom(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        taskPauseExecute: BuildTaskPauseInfo
    ): Result<Boolean> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val pipelineBuildInfo =
            pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: throw ParamBlankException("Invalid buildId")
        return Result(
            pipelinePauseBuildFacadeService.executePauseAtom(
                userId = checkUserId(userId),
                projectId = checkProjectId(projectId),
                pipelineId = checkPipelineId(pipelineId, pipelineBuildInfo.pipelineId),
                buildId = buildId,
                isContinue = taskPauseExecute.isContinue,
                taskId = taskPauseExecute.taskId,
                element = taskPauseExecute.element,
                stageId = taskPauseExecute.stageId,
                containerId = taskPauseExecute.containerId
            )
        )
    }

    private fun checkProjectId(projectId: String): String {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        return projectId
    }

    private fun checkPipelineId(pipelineId: String?): String {
        if (pipelineId.isNullOrBlank()) {
            throw ParamBlankException("PipelineId is invalid or null ")
        }
        return pipelineId
    }

    private fun checkPipelineId(pipelineId: String?, pipelineIdFormDB: String): String {
        if (pipelineId != null && pipelineId != pipelineIdFormDB) {
            throw ParamBlankException("PipelineId is invalid ")
        }
        return pipelineIdFormDB
    }

    private fun checkUserId(userId: String): String {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        return userId
    }
}

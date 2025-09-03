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

package com.tencent.devops.process.api.app

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.pipeline.pojo.cascade.BuildCascadeProps
import com.tencent.devops.common.quality.pojo.request.QualityReviewRequest
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineBuildQualityService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.service.app.AppBuildService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import org.apache.commons.lang3.math.NumberUtils
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL", "UNUSED")
@RestResource
class AppPipelineBuildResourceImpl @Autowired constructor(
    private val appBuildService: AppBuildService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineBuildQualityService: PipelineBuildQualityService,
    private val pipelineRuntimeService: PipelineRuntimeService
) : AppPipelineBuildResource {

    override fun manualQualityGateReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        action: ManualReviewAction,
        request: QualityReviewRequest?
    ): Result<Boolean> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (elementId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS
        pipelineBuildQualityService.buildManualQualityGateReview(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            elementId = elementId,
            action = action,
            channelCode = channelCode,
            ruleIds = request?.ruleIds
        )
        return Result(true)
    }

    override fun manualReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        params: ReviewParam
    ): Result<Boolean> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (elementId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS
        pipelineBuildFacadeService.buildManualReview(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            elementId = elementId,
            params = params,
            channelCode = ChannelCode.BS,
            checkPermission = ChannelCode.isNeedAuth(channelCode),
            stepId = null
        )
        return Result(true)
    }

    override fun buildTriggerReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        approve: Boolean,
        channelCode: ChannelCode
    ): Result<Boolean> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            pipelineBuildFacadeService.buildTriggerReview(
                userId = userId,
                buildId = buildId,
                pipelineId = pipelineId,
                projectId = projectId,
                approve = approve,
                channelCode = channelCode,
                checkPermission = ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    override fun manualStartStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        cancel: Boolean?,
        reviewRequest: StageReviewRequest?
    ): Result<Boolean> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (stageId.isBlank()) {
            throw ParamBlankException("Invalid stageId")
        }

        pipelineBuildFacadeService.buildManualStartStage(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            isCancel = cancel ?: false,
            reviewRequest = reviewRequest
        )
        return Result(true)
    }

    override fun getHistoryBuildNew(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        materialAlias: List<String>?,
        materialUrl: String?,
        materialBranch: List<String>?,
        materialCommitId: String?,
        materialCommitMessage: String?,
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?,
        totalTimeMin: Long?,
        totalTimeMax: Long?,
        remark: String?,
        buildNoStart: Int?,
        buildNoEnd: Int?,
        buildMsg: String?,
        debug: Boolean?,
        triggerAlias: List<String>?,
        triggerBranch: List<String>?,
        triggerUser: List<String>?
    ): Result<BuildHistoryPage<BuildHistory>> {
        checkParam(userId, projectId, pipelineId, pageSize)
        val result = pipelineBuildFacadeService.getHistoryBuild(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            page = page,
            pageSize = pageSize,
            materialAlias = materialAlias,
            materialUrl = materialUrl,
            materialBranch = materialBranch,
            materialCommitId = materialCommitId,
            materialCommitMessage = materialCommitMessage,
            status = status,
            trigger = trigger,
            queueTimeStartTime = queueTimeStartTime,
            queueTimeEndTime = queueTimeEndTime,
            startTimeStartTime = startTimeStartTime,
            startTimeEndTime = startTimeEndTime,
            endTimeStartTime = endTimeStartTime,
            endTimeEndTime = endTimeEndTime,
            totalTimeMin = totalTimeMin,
            totalTimeMax = totalTimeMax,
            remark = remark,
            buildNoStart = buildNoStart,
            buildNoEnd = buildNoEnd,
            buildMsg = buildMsg,
            debug = debug,
            triggerAlias = triggerAlias,
            triggerBranch = triggerBranch,
            triggerUser = triggerUser
        )
        return Result(result)
    }

    override fun getBuildParameters(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        archiveFlag: Boolean?
    ): Result<List<BuildParameters>> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            pipelineBuildFacadeService.getBuildParameters(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                archiveFlag = archiveFlag
            )
        )
    }

    override fun goToReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String
    ): Result<ReviewParam> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (elementId.isBlank()) {
            throw ParamBlankException("Invalid elementId")
        }

        return Result(
            pipelineBuildFacadeService.goToReview(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                elementId = elementId
            )
        )
    }

    override fun manualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?
    ): Result<BuildManualStartupInfo> {
        checkParam(userId, projectId, pipelineId)

        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS

        return Result(
            pipelineBuildFacadeService.buildManualStartupInfo(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                channelCode = channelCode
            ).apply {
                // TODO app暂时无法同步特性，临时方案为buildNo覆盖为currentBuildNo
                buildNo?.currentBuildNo?.let { buildNo?.buildNo = it }
                // 分支接口要改为app接口
                properties.forEach {
                    it.searchUrl?.let { searchUrl ->
                        if (searchUrl.contains("/api/user/buildParam")) {
                            it.searchUrl =
                                searchUrl.replace("/api/user/buildParam", "/api/app/pipelineBuild/buildParam")
                        }
                    }
                    replaceBuildCascadePropsSearchUrl(it.cascadeProps)
                }
            }
        )
    }

    // 替换BuildCascadeProps以及子节点的的searchUrl
    private fun replaceBuildCascadePropsSearchUrl(buildCascadeProps: BuildCascadeProps?) {
        if (buildCascadeProps == null) {
            return
        }
        buildCascadeProps.searchUrl?.let { searchUrl ->
            if (searchUrl.contains("/api/user/buildParam")) {
                buildCascadeProps.searchUrl =
                    searchUrl.replace("/api/user/buildParam", "/api/app/pipelineBuild/buildParam")
            }
        }
        replaceBuildCascadePropsSearchUrl(buildCascadeProps.children)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_EXECUTE)
    override fun manualStartup(
        userId: String,
        projectId: String,
        pipelineId: String,
        values: Map<String, String>,
        version: Int?
    ): Result<BuildId> {
        checkParam(userId, projectId, pipelineId)

        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS

        val buildNo = values["buildNo"]?.let { NumberUtils.toInt(it) }

        return Result(
            pipelineBuildFacadeService.buildManualStartup(
                userId = userId,
                startType = StartType.MANUAL,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                values = values.filter { it.key != "buildNo" },
                channelCode = channelCode,
                buildNo = buildNo
            )
        )
    }

    override fun manualShutdown(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<Boolean> {
        checkParam(userId, projectId, pipelineId)

        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }

        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS

        pipelineBuildFacadeService.buildManualShutdown(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = channelCode
        )
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_EXECUTE)
    override fun retry(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String?,
        failedContainer: Boolean?,
        skipFailedTask: Boolean?
    ): Result<BuildId> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }

        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS

        return Result(
            pipelineBuildFacadeService.retry(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId,
                failedContainer = failedContainer,
                skipFailedTask = skipFailedTask,
                channelCode = channelCode
            )
        )
    }

    private fun checkParam(userId: String, projectId: String, pipelineId: String, pageSize: Int? = null) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (pageSize != null && pageSize > 1000) {
            throw ParamBlankException("PageSize could not be greater than 1000")
        }
    }
}

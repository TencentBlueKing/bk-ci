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
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryRemark
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import com.tencent.devops.process.service.PipelineRecentUseService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.service.builds.PipelineBuildMaintainFacadeService
import com.tencent.devops.process.service.builds.PipelinePauseBuildFacadeService
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
@Suppress("ALL")
class UserBuildResourceImpl @Autowired constructor(
    private val pipelineBuildMaintainFacadeService: PipelineBuildMaintainFacadeService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelinePauseBuildFacadeService: PipelinePauseBuildFacadeService,
    private val pipelineRecentUseService: PipelineRecentUseService
) : UserBuildResource {

    override fun manualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<BuildManualStartupInfo> {
        checkParam(userId, projectId, pipelineId)
        return Result(pipelineBuildFacadeService.buildManualStartupInfo(userId, projectId, pipelineId, ChannelCode.BS))
    }

    override fun getBuildParameters(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<List<BuildParameters>> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(pipelineBuildFacadeService.getBuildParameters(userId, projectId, pipelineId, buildId))
    }

    override fun manualStartup(
        userId: String,
        projectId: String,
        pipelineId: String,
        values: Map<String, String>,
        buildNo: Int?,
        triggerReviewers: List<String>?
    ): Result<BuildId> {
        checkParam(userId, projectId, pipelineId)
        val manualStartup = pipelineBuildFacadeService.buildManualStartup(
            userId = userId,
            startType = StartType.MANUAL,
            projectId = projectId,
            pipelineId = pipelineId,
            values = values,
            channelCode = ChannelCode.BS,
            buildNo = buildNo,
            triggerReviewers = triggerReviewers
        )
        pipelineRecentUseService.record(userId, projectId, pipelineId)
        return Result(manualStartup)
    }

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
        return Result(
            pipelineBuildFacadeService.retry(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId,
                failedContainer = failedContainer,
                skipFailedTask = skipFailedTask
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
        pipelineBuildFacadeService.buildManualShutdown(userId, projectId, pipelineId, buildId, ChannelCode.BS)
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
        pipelineBuildFacadeService.buildManualReview(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            elementId = elementId,
            params = params,
            channelCode = ChannelCode.BS,
            checkPermission = ChannelCode.isNeedAuth(ChannelCode.BS)
        )
        return Result(true)
    }

    override fun buildTriggerReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        approve: Boolean
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
                approve = approve
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
            throw ParamBlankException("Invalid buildId")
        }

        return Result(pipelineBuildFacadeService.goToReview(userId, projectId, pipelineId, buildId, elementId))
    }

    @Timed
    override fun getBuildDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<ModelDetail> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        val buildDetail = pipelineBuildFacadeService.getBuildDetail(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = ChannelCode.BS
        )
        pipelineRecentUseService.record(userId, projectId, pipelineId)
        return Result(buildDetail)
    }

    override fun getBuildRecordByExecuteCount(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?
    ): Result<ModelRecord> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            pipelineBuildFacadeService.getBuildRecord(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = executeCount,
                channelCode = ChannelCode.BS
            )
        )
    }

    override fun getBuildDetailByBuildNo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNo: Int
    ): Result<ModelDetail> {
        checkParam(userId, projectId, pipelineId)
        if (buildNo <= 0) {
            throw ParamBlankException("Invalid buildNo")
        }
        return Result(
            pipelineBuildFacadeService.getBuildDetailByBuildNo(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildNo = buildNo,
                channelCode = ChannelCode.BS
            )
        )
    }

    override fun getBuildRecordByBuildNum(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNum: Int
    ): Result<ModelRecord> {
        checkParam(userId, projectId, pipelineId)
        if (buildNum <= 0) {
            throw ParamBlankException("Invalid buildNo")
        }
        return Result(
            pipelineBuildFacadeService.getBuildRecordByBuildNum(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildNum = buildNum,
                channelCode = ChannelCode.BS
            )
        )
    }

    override fun goToLatestFinishedBuild(userId: String, projectId: String, pipelineId: String): Response {
        checkParam(userId = userId, projectId = projectId, pipelineId = pipelineId)
        return pipelineBuildFacadeService.goToLatestFinishedBuild(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS,
            checkPermission = false
        )
    }

    override fun getHistoryBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        checkPermission: Boolean?
    ): Result<BuildHistoryPage<BuildHistory>> {
        checkParam(userId, projectId, pipelineId)
        val check = checkPermission ?: true
        val result = pipelineBuildFacadeService.getHistoryBuild(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            page = page,
            pageSize = pageSize,
            channelCode = ChannelCode.BS,
            checkPermission = check
        )
        return Result(result)
    }

    @Timed
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
        buildMsg: String?
    ): Result<BuildHistoryPage<BuildHistory>> {
        checkParam(userId, projectId, pipelineId)
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
            buildMsg = buildMsg
        )
        pipelineRecentUseService.record(userId, projectId, pipelineId)
        return Result(result)
    }

    override fun updateBuildHistoryRemark(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        remark: BuildHistoryRemark?
    ): Result<Boolean> {
        checkParam(userId, projectId, pipelineId)
        pipelineBuildFacadeService.updateRemark(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            remark = remark?.remark
        )
        return Result(true)
    }

    override fun getHistoryConditionStatus(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<IdValue>> {
        checkParam(userId, projectId, pipelineId)
        return Result(pipelineBuildFacadeService.getHistoryConditionStatus(userId, projectId, pipelineId))
    }

    override fun getHistoryConditionTrigger(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<IdValue>> {
        checkParam(userId, projectId, pipelineId)
        return Result(pipelineBuildFacadeService.getHistoryConditionTrigger(userId, projectId, pipelineId))
    }

    override fun getHistoryConditionRepo(userId: String, projectId: String, pipelineId: String): Result<List<String>> {
        checkParam(userId, projectId, pipelineId)
        return Result(pipelineBuildFacadeService.getHistoryConditionRepo(userId, projectId, pipelineId))
    }

    override fun getHistoryConditionBranch(
        userId: String,
        projectId: String,
        pipelineId: String,
        alias: List<String>?
    ): Result<List<String>> {
        checkParam(userId, projectId, pipelineId)
        return Result(
            pipelineBuildFacadeService.getHistoryConditionBranch(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                alias = alias
            )
        )
    }

    override fun executionPauseAtom(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        element: Element?,
        isContinue: Boolean,
        stageId: String,
        containerId: String
    ): Result<Boolean> {
        checkParam(userId, projectId, pipelineId)
        return Result(
            pipelinePauseBuildFacadeService.executePauseAtom(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                isContinue = isContinue,
                taskId = taskId,
                element = element,
                stageId = stageId,
                containerId = containerId
            )
        )
    }

    override fun tryFinishStuckBuilds(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildIds: Set<String>
    ): Result<Boolean> {
        return Result(
            data = pipelineBuildMaintainFacadeService.tryFinishStuckBuilds(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildIds = buildIds
            )
        )
    }

    private fun checkParam(userId: String, projectId: String, pipelineId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}

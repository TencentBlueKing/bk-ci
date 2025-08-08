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

package com.tencent.devops.process.api

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserBuildResource
import com.tencent.devops.process.engine.service.PipelineProgressRateService
import com.tencent.devops.process.enums.HistorySearchType
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryRemark
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.BuildStageProgressInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.pipeline.BuildRecordInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import com.tencent.devops.process.service.PipelineRecentUseService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.service.builds.PipelineBuildMaintainFacadeService
import com.tencent.devops.process.service.builds.PipelinePauseBuildFacadeService
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import jakarta.ws.rs.core.Response

@RestResource
@Suppress("ALL")
class UserBuildResourceImpl @Autowired constructor(
    private val pipelineBuildMaintainFacadeService: PipelineBuildMaintainFacadeService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelinePauseBuildFacadeService: PipelinePauseBuildFacadeService,
    private val pipelineRecentUseService: PipelineRecentUseService,
    private val pipelineProgressRateService: PipelineProgressRateService
) : UserBuildResource {

    override fun manualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?
    ): Result<BuildManualStartupInfo> {
        checkParam(userId, projectId, pipelineId)
        return Result(
            pipelineBuildFacadeService.buildManualStartupInfo(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                channelCode = ChannelCode.BS
            )
        )
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

    @AuditEntry(actionId = ActionId.PIPELINE_EXECUTE)
    override fun manualStartup(
        userId: String,
        projectId: String,
        pipelineId: String,
        values: Map<String, String>,
        buildNo: Int?,
        triggerReviewers: List<String>?,
        version: Int?
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
            version = version,
            triggerReviewers = triggerReviewers
        )
        pipelineRecentUseService.record(userId, projectId, pipelineId)
        return Result(manualStartup)
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
            checkPermission = ChannelCode.isNeedAuth(ChannelCode.BS),
            stepId = null
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
    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
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

    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
    override fun getBuildRecordByExecuteCount(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?,
        archiveFlag: Boolean?
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
                channelCode = ChannelCode.BS,
                archiveFlag = archiveFlag
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
    override fun getBuildRecordInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<List<BuildRecordInfo>> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            pipelineBuildFacadeService.getBuildRecordInfo(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.BS
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
    override fun getBuildDetailByBuildNo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNo: Int,
        debugVersion: Int?
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
                channelCode = ChannelCode.BS,
                debugVersion = debugVersion
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
    override fun getBuildRecordByBuildNum(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNum: Int,
        debugVersion: Int?,
        archiveFlag: Boolean?
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
                channelCode = ChannelCode.BS,
                debugVersion = debugVersion,
                archiveFlag = archiveFlag
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
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

    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
    override fun getHistoryBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        checkPermission: Boolean?,
        debugVersion: Int?
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
            checkPermission = check,
            debugVersion = debugVersion
        )
        return Result(result)
    }

    @Timed
    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
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
        archiveFlag: Boolean?,
        debug: Boolean?,
        triggerAlias: List<String>?,
        triggerBranch: List<String>?,
        triggerUser: List<String>?
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
            buildMsg = buildMsg,
            archiveFlag = archiveFlag,
            debug = debug,
            triggerAlias = triggerAlias,
            triggerBranch = triggerBranch,
            triggerUser = triggerUser
        )
        if (archiveFlag != true) {
            pipelineRecentUseService.record(userId, projectId, pipelineId)
        }
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

    override fun getHistoryConditionRepo(
        userId: String,
        projectId: String,
        pipelineId: String,
        debugVersion: Int?,
        search: String?,
        type: HistorySearchType?
    ): Result<List<String>> {
        checkParam(userId, projectId, pipelineId)
        return Result(
            pipelineBuildFacadeService.getHistoryConditionRepo(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                debugVersion = debugVersion,
                search = search,
                type = type
            )
        )
    }

    override fun getHistoryConditionBranch(
        userId: String,
        projectId: String,
        pipelineId: String,
        alias: List<String>?,
        debugVersion: Int?,
        search: String?,
        type: HistorySearchType?
    ): Result<List<String>> {
        checkParam(userId, projectId, pipelineId)
        return Result(
            pipelineBuildFacadeService.getHistoryConditionBranch(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                alias = alias,
                debugVersion = debugVersion,
                search = search,
                type = type
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
                containerId = containerId,
                stepId = null
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

    override fun getStageProgressRate(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String
    ): Result<BuildStageProgressInfo> {
        return Result(
            pipelineProgressRateService.calculateStageProgressRate(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId
            )
        )
    }

    override fun replayByBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        forceTrigger: Boolean?
    ): Result<BuildId> {
        return Result(
            pipelineBuildFacadeService.replayBuild(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = userId,
                forceTrigger = forceTrigger ?: false
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

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

package com.tencent.devops.process.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.UserBuildResource
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryRemark
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class UserBuildResourceImpl @Autowired constructor(private val buildService: PipelineBuildService) : UserBuildResource {

    override fun manualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<BuildManualStartupInfo> {
        checkParam(userId, projectId, pipelineId)
        return Result(buildService.buildManualStartupInfo(userId, projectId, pipelineId, ChannelCode.BS))
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
        return Result(buildService.getBuildParameters(userId, projectId, pipelineId, buildId))
    }

    override fun manualStartup(
        userId: String,
        projectId: String,
        pipelineId: String,
        values: Map<String, String>
    ): Result<BuildId> {
        checkParam(userId, projectId, pipelineId)
        return Result(
            BuildId(
                buildService.buildManualStartup(
                    userId,
                    StartType.MANUAL,
                    projectId,
                    pipelineId,
                    values,
                    ChannelCode.BS
                )
            )
        )
    }

    override fun retry(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String?
    ): Result<BuildId> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(BuildId(buildService.retry(userId, projectId, pipelineId, buildId, taskId)))
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
        buildService.buildManualShutdown(userId, projectId, pipelineId, buildId, ChannelCode.BS)
        return Result(true)
    }

    override fun manualReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        action: ManualReviewAction
    ): Result<Boolean> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (elementId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        buildService.buildManualReview(
            userId,
            projectId,
            pipelineId,
            buildId,
            elementId,
            action,
            ChannelCode.BS,
            ChannelCode.isNeedAuth(ChannelCode.BS)
        )
        return Result(true)
    }

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
        return Result(
            buildService.getBuildDetail(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
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
        return Result(buildService.getBuildDetailByBuildNo(userId, projectId, pipelineId, buildNo, ChannelCode.BS))
    }

    override fun goToLatestFinishedBuild(userId: String, projectId: String, pipelineId: String): Response {
        checkParam(userId, projectId, pipelineId)
        return buildService.goToLatestFinishedBuild(userId, projectId, pipelineId, ChannelCode.BS, false)
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
        val result = buildService.getHistoryBuild(
            userId, projectId, pipelineId,
            page, pageSize, ChannelCode.BS, check
        )
        return Result(result)
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
        remark: String?
    ): Result<BuildHistoryPage<BuildHistory>> {
        checkParam(userId, projectId, pipelineId)
        val result = buildService.getHistoryBuild(
            userId,
            projectId,
            pipelineId,
            page,
            pageSize,
            materialAlias,
            materialUrl,
            materialBranch,
            materialCommitId,
            materialCommitMessage,
            status,
            trigger,
            queueTimeStartTime,
            queueTimeEndTime,
            startTimeStartTime,
            startTimeEndTime,
            endTimeStartTime,
            endTimeEndTime,
            totalTimeMin,
            totalTimeMax,
            remark
        )
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
        buildService.updateRemark(userId, projectId, pipelineId, buildId, remark?.remark)
        return Result(true)
    }

    override fun getHistoryConditionStatus(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<IdValue>> {
        checkParam(userId, projectId, pipelineId)
        return Result(buildService.getHistoryConditionStatus(userId, projectId, pipelineId))
    }

    override fun getHistoryConditionTrigger(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<IdValue>> {
        checkParam(userId, projectId, pipelineId)
        return Result(buildService.getHistoryConditionTrigger(userId, projectId, pipelineId))
    }

    override fun getHistoryConditionRepo(userId: String, projectId: String, pipelineId: String): Result<List<String>> {
        checkParam(userId, projectId, pipelineId)
        return Result(buildService.getHistoryConditionRepo(userId, projectId, pipelineId))
    }

    override fun getHistoryConditionBranch(
        userId: String,
        projectId: String,
        pipelineId: String,
        alias: List<String>?
    ): Result<List<String>> {
        checkParam(userId, projectId, pipelineId)
        return Result(buildService.getHistoryConditionBranch(userId, projectId, pipelineId, alias))
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

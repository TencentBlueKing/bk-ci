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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.SimpleResult
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineVMBuildService
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryVariables
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.pipeline.PipelineLatestBuild
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceBuildResourceImpl @Autowired constructor(
    private val buildService: PipelineBuildService,
    private val vmBuildService: PipelineVMBuildService
) : ServiceBuildResource {

    override fun setVMStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        status: BuildStatus,
        errorType: ErrorType?,
        errorCode: Int?,
        errorMsg: String?
    ): Result<Boolean> {
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(vmBuildService.setStartUpVMStatus(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            buildStatus = status,
            errorType = errorType,
            errorCode = errorCode,
            errorMsg = errorMsg
        ))
    }

    override fun vmStarted(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        vmName: String
    ): Result<Boolean> {
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(vmBuildService.vmStartedByDispatch(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            vmName = vmName
        ))
    }

    override fun manualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ): Result<BuildManualStartupInfo> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        return Result(
            buildService.buildManualStartupInfo(
                userId, projectId, pipelineId,
                channelCode, ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    override fun manualStartup(
        userId: String,
        projectId: String,
        pipelineId: String,
        values: Map<String, String>,
        channelCode: ChannelCode,
        buildNo: Int?
    ): Result<BuildId> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        return Result(
            BuildId(
                buildService.buildManualStartup(
                    userId = userId,
                    startType = StartType.SERVICE,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    values = values,
                    channelCode = channelCode,
                    buildNo = buildNo,
                    checkPermission = ChannelCode.isNeedAuth(channelCode),
                    frequencyLimit = false
                )
            )
        )
    }

    override fun retry(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String?,
        channelCode: ChannelCode
    ): Result<BuildId> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(BuildId(buildService.retry(userId, projectId, pipelineId, buildId, taskId, false, channelCode, ChannelCode.isNeedAuth(channelCode))))
    }

    override fun manualShutdown(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): Result<Boolean> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        buildService.buildManualShutdown(
            userId, projectId, pipelineId, buildId, channelCode,
            ChannelCode.isNeedAuth(channelCode)
        )
        return Result(true)
    }

    override fun serviceShutdown(
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): Result<Boolean> {
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        buildService.serviceShutdown(projectId, pipelineId, buildId, channelCode)
        return Result(true)
    }

    override fun manualReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        params: ReviewParam,
        channelCode: ChannelCode
    ): Result<Boolean> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (elementId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        buildService.buildManualReview(
            userId, projectId, pipelineId, buildId, elementId,
            params, channelCode, ChannelCode.isNeedAuth(channelCode)
        )
        return Result(true)
    }

    override fun getBuildDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): Result<ModelDetail> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            buildService.getBuildDetail(
                userId, projectId, pipelineId, buildId, channelCode,
                ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    override fun getHistoryBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode
    ): Result<BuildHistoryPage<BuildHistory>> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        val result = buildService.getHistoryBuild(
            userId, projectId, pipelineId,
            page, pageSize, channelCode, ChannelCode.isNeedAuth(channelCode)
        )
        return Result(result)
    }

    override fun serviceBasic(buildId: String): Result<BuildBasicInfo> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(buildService.serviceBuildBasicInfo(buildId))
    }

    override fun getBuildStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): Result<BuildHistoryWithVars> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            buildService.getBuildStatusWithVars(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = channelCode,
                checkPermission = ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    /**
     * 不鉴权接口，仅供平台方调用
     */
    override fun getBuildStatusWithoutPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): Result<BuildHistoryWithVars> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            buildService.getBuildStatusWithVars(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = channelCode,
                checkPermission = false
            )
        )
    }

    override fun getBuildDetailStatusWithoutPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): Result<String> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            buildService.getBuildDetailStatus(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = channelCode,
                checkPermission = false
            )
        )
    }

    override fun getBuildVars(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): Result<BuildHistoryVariables> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return buildService.getBuildVars(userId, projectId, pipelineId, buildId, ChannelCode.isNeedAuth(channelCode))
    }

    override fun batchServiceBasic(buildIds: Set<String>): Result<Map<String, BuildBasicInfo>> {
        if (buildIds.isEmpty()) return Result(mapOf())
        return Result(buildService.batchServiceBasic(buildIds))
    }

    override fun getBatchBuildStatus(
        projectId: String,
        buildId: Set<String>,
        channelCode: ChannelCode
    ): Result<List<BuildHistory>> {
        if (buildId.isEmpty()) {
            return Result(listOf())
        }
        return Result(
            buildService.getBatchBuildStatus(
                projectId, buildId,
                channelCode, ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    override fun getPipelineLatestBuildByIds(
        projectId: String,
        pipelineIds: List<String>
    ): Result<Map<String, PipelineLatestBuild>> {
        return Result(buildService.getPipelineLatestBuildByIds(projectId, pipelineIds))
    }

    override fun saveBuildVmInfo(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        vmInfo: VmInfo
    ): Result<Boolean> {
        buildService.saveBuildVmInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            vmInfo = vmInfo
        )
        return Result(true)
    }

    override fun getSingleHistoryBuild(projectId: String, pipelineId: String, buildNum: String, channelCode: ChannelCode?): Result<BuildHistory?> {
        val history = buildService.getSingleHistoryBuild(
            projectId, pipelineId,
            buildNum.toInt(), channelCode ?: ChannelCode.BS
        )
        return Result(history)
    }

    override fun workerBuildFinish(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        simpleResult: SimpleResult
    ): Result<Boolean> {
        buildService.workerBuildFinish(
            projectCode = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            simpleResult = simpleResult
        )
        return Result(true)
    }

    override fun manualStartStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        cancel: Boolean?
    ): Result<Boolean> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (stageId.isBlank()) {
            throw ParamBlankException("Invalid stageId")
        }

        buildService.buildManualStartStage(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            isCancel = cancel ?: false
        )
        return Result(true)
    }

    private fun checkParam(projectId: String, pipelineId: String) {
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun checkUserId(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}

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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwBuildResourceV4
import com.tencent.devops.openapi.service.IndexService
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryRemark
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.BuildTaskPauseInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwBuildResourceV4Impl @Autowired constructor(
    private val client: Client,
    private val apiGatewayUtil: ApiGatewayUtil,
    private val indexService: IndexService
) : ApigwBuildResourceV4 {
    override fun manualStartupInfo(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<BuildManualStartupInfo> {
        logger.info("OPENAPI_BUILD_V4|$userId|manual startup info|$projectId|$pipelineId")
        return client.get(ServiceBuildResource::class).manualStartupInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun detail(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String
    ): Result<ModelDetail> {
        logger.info("OPENAPI_BUILD_V4|$userId|detail|$projectId|$pipelineId|$buildId")
        return client.get(ServiceBuildResource::class).getBuildDetail(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun getHistoryBuild(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        updateTimeDesc: Boolean?,
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
        startUser: List<String>?
    ): Result<BuildHistoryPage<BuildHistory>> {
        logger.info(
            "OPENAPI_BUILD_V4|$userId|get history build|$projectId|$pipelineId|$page|$pageSize" +
                "|$updateTimeDesc|materialAlias=$materialAlias|$materialUrl|$materialBranch|$materialCommitId" +
                "|$materialCommitMessage|status=$status|$trigger|$queueTimeStartTime|$queueTimeEndTime" +
                "|$startTimeStartTime|startTimeEndTime=$startTimeEndTime|$endTimeStartTime|$endTimeEndTime" +
                "|$totalTimeMin|$totalTimeMax|remark=$remark|$buildNoStart|$buildNoEnd|$buildMsg|$startUser"
        )
        return client.get(ServiceBuildResource::class).getHistoryBuild(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            channelCode = apiGatewayUtil.getChannelCode(),
            updateTimeDesc = updateTimeDesc,
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
            startUser = startUser
        )
    }

    override fun start(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        values: Map<String, String>?,
        buildNo: Int?
    ): Result<BuildId> {
        logger.info("OPENAPI_BUILD_V4|$userId|start|$projectId|$pipelineId|$values|$buildNo")
        return client.get(ServiceBuildResource::class).manualStartupNew(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            values = values ?: emptyMap(),
            buildNo = buildNo,
            channelCode = apiGatewayUtil.getChannelCode(),
            startType = StartType.SERVICE
        )
    }

    override fun stop(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_BUILD_V4|$userId|stop|$projectId|$pipelineId|$buildId")
        return client.get(ServiceBuildResource::class).manualShutdown(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun retry(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String?,
        buildNumber: Int?,
        taskId: String?,
        failedContainer: Boolean?,
        skipFailedTask: Boolean?
    ): Result<BuildId> {
        logger.info(
            "OPENAPI_BUILD_V4|$userId|retry|$projectId|$pipelineId|$buildId|$taskId|$failedContainer" +
                "|$skipFailedTask"
        )

        val checkPipelineId = if (buildId.isNullOrBlank()) {
            pipelineId ?: throw ParamBlankException("pipelineId and buildId cannot be empty at the same time")
        } else checkPipelineId(projectId, pipelineId, buildId)

        val checkBuildId = if (buildId.isNullOrBlank()) {
            val buildNum = buildNumber
                ?: throw ParamBlankException("buildId and buildNumber cannot be empty at the same time")
            checkBuildId(projectId, checkPipelineId, buildNum)
        } else buildId

        return client.get(ServiceBuildResource::class).retry(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId,
            buildId = checkBuildId,
            taskId = taskId,
            failedContainer = failedContainer,
            skipFailedTask = skipFailedTask,
            channelCode = apiGatewayUtil.getChannelCode(),
            checkManualStartup = true
        )
    }

    override fun getStatus(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String
    ): Result<BuildHistoryWithVars> {
        logger.info("OPENAPI_BUILD_V4|$userId|get status|$projectId|$pipelineId|$buildId")
        return client.get(ServiceBuildResource::class).getBuildStatus(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun manualStartStage(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        stageId: String,
        cancel: Boolean?,
        reviewRequest: StageReviewRequest?
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_BUILD_V4|$userId|manual start stage|$projectId|$pipelineId|$buildId|$stageId|$cancel" +
                "|$reviewRequest"
        )
        return client.get(ServiceBuildResource::class).manualStartStage(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            stageId = stageId,
            cancel = cancel ?: false,
            reviewRequest = reviewRequest
        )
    }

    override fun getVariableValue(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        variableNames: List<String>
    ): Result<Map<String, String>> {
        logger.info("OPENAPI_BUILD_V4|$userId|get variable value|$projectId|$pipelineId|$buildId|$variableNames")
        return client.get(ServiceBuildResource::class).getBuildVariableValue(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            variableNames = variableNames
        )
    }

    override fun executionPauseAtom(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        taskPauseExecute: BuildTaskPauseInfo
    ): Result<Boolean> {
        logger.info("OPENAPI_BUILD_V4|$userId|execution pause atom|$projectId|$pipelineId|$buildId|$taskPauseExecute")
        return client.get(ServiceBuildResource::class).executionPauseAtom(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            taskPauseExecute = taskPauseExecute
        )
    }

    override fun buildRestart(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String
    ): Result<String> {
        logger.info("OPENAPI_BUILD_V4|$userId|build restart|$projectId|$pipelineId|$buildId")
        return client.get(ServiceBuildResource::class).buildRestart(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId
        )
    }

    override fun updateRemark(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        remark: BuildHistoryRemark?
    ): Result<Boolean> {
        logger.info("OPENAPI_BUILD_V4|$userId|update remark|$projectId|$pipelineId|$buildId")
        return client.get(ServiceBuildResource::class).updateRemark(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            remark = remark
        )
    }

    override fun manualReview(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String,
        elementId: String,
        params: ReviewParam
    ): Result<Boolean> {
        logger.info("OPENAPI_BUILD_V4|$userId|manual review|$projectId|$pipelineId|$buildId|$elementId|$params")
        return client.get(ServiceBuildResource::class).manualReview(
            userId = userId,
            projectId = projectId,
            pipelineId = checkPipelineId(projectId, pipelineId, buildId),
            buildId = buildId,
            elementId = elementId,
            params = params,
            channelCode = ChannelCode.BS
        )
    }

    override fun manualStartupOptions(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        search: String?,
        property: BuildFormProperty
    ): Result<List<BuildFormValue>> {
        return client.get(ServiceBuildResource::class).manualSearchOptions(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            search = search,
            buildFormProperty = property
        )
    }

    override fun tryFinishStuckBuilds(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildIds: Set<String>
    ): Result<Boolean> {
        logger.info("OPENAPI_BUILD_V4|$userId|tryFinishStuckBuilds|$projectId|$pipelineId|$buildIds")
        return client.get(ServiceBuildResource::class).tryFinishStuckBuilds(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildIds = buildIds
        )
    }

    private fun checkPipelineId(projectId: String, pipelineId: String?, buildId: String): String {
        val pipelineIdFormDB = indexService.getHandle(buildId) {
            kotlin.runCatching {
                client.get(ServiceBuildResource::class).getPipelineIdFromBuildId(projectId, buildId).data
            }.getOrElse {
                throw ParamBlankException(
                    it.message ?: "Invalid buildId, please check if projectId & buildId are related"
                )
            } ?: throw ParamBlankException("Invalid buildId")
        }
        if (pipelineId != null && pipelineId != pipelineIdFormDB) {
            throw ParamBlankException("PipelineId is invalid ")
        }
        return pipelineIdFormDB
    }

    private fun checkBuildId(projectId: String, pipelineId: String, buildNumber: Int): String {
        return client.get(ServiceBuildResource::class)
            .getBuildIdFromBuildNumber(projectId, pipelineId, buildNumber).data
            ?: throw ParamBlankException("Invalid buildNumber")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwBuildResourceV4Impl::class.java)
    }
}

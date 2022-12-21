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
package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwBuildResourceV3
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.BuildTaskPauseInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwBuildResourceV3Impl @Autowired constructor(
    private val client: Client,
    private val apiGatewayUtil: ApiGatewayUtil
) : ApigwBuildResourceV3 {
    override fun manualStartupInfo(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<BuildManualStartupInfo> {
        logger.info("OPENAPI_BUILD_V3|$userId|manual startup info|$projectId|$pipelineId")
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
        pipelineId: String,
        buildId: String
    ): Result<ModelDetail> {
        logger.info("OPENAPI_BUILD_V3|$userId|detail|$projectId|$pipelineId|$buildId")
        return client.get(ServiceBuildResource::class).getBuildDetail(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
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
        updateTimeDesc: Boolean?
    ): Result<BuildHistoryPage<BuildHistory>> {
        logger.info("OPENAPI_BUILD_V3|$userId|get history build|$projectId|$pipelineId|$page|$pageSize|$updateTimeDesc")
        return client.get(ServiceBuildResource::class).getHistoryBuild(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            channelCode = apiGatewayUtil.getChannelCode(),
            updateTimeDesc = updateTimeDesc,
            materialAlias = null,
            materialUrl = null,
            materialBranch = null,
            materialCommitId = null,
            materialCommitMessage = null,
            status = null,
            trigger = null,
            queueTimeStartTime = null,
            queueTimeEndTime = null,
            startTimeStartTime = null,
            startTimeEndTime = null,
            endTimeStartTime = null,
            endTimeEndTime = null,
            totalTimeMin = null,
            totalTimeMax = null,
            remark = null,
            buildNoStart = null,
            buildNoEnd = null,
            buildMsg = null,
            startUser = null
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
        logger.info("OPENAPI_BUILD_V3|$userId|start|$projectId|$pipelineId|$values|$buildNo")
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
        pipelineId: String,
        buildId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_BUILD_V3|$userId|stop|$projectId|$pipelineId|$buildId")
        return client.get(ServiceBuildResource::class).manualShutdown(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun retry(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String?,
        failedContainer: Boolean?,
        skipFailedTask: Boolean?
    ): Result<BuildId> {
        logger.info(
            "OPENAPI_BUILD_V3|$userId|retry|$projectId|$pipelineId|$buildId|$taskId|$failedContainer" +
                "|$skipFailedTask"
        )
        return client.get(ServiceBuildResource::class).retry(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
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
        pipelineId: String,
        buildId: String
    ): Result<BuildHistoryWithVars> {
        logger.info("OPENAPI_BUILD_V3|$userId|get status|$projectId|$pipelineId|$buildId")
        return client.get(ServiceBuildResource::class).getBuildStatus(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun manualStartStage(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        cancel: Boolean?,
        reviewRequest: StageReviewRequest?
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_BUILD_V3|$userId|manual start stage|$projectId|$pipelineId|$buildId|$stageId|$cancel" +
                "|$reviewRequest"
        )
        return client.get(ServiceBuildResource::class).manualStartStage(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
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
        pipelineId: String,
        buildId: String,
        variableNames: List<String>
    ): Result<Map<String, String>> {
        logger.info("OPENAPI_BUILD_V3|$userId|get variable value|$projectId|$pipelineId|$buildId|$variableNames")
        return client.get(ServiceBuildResource::class).getBuildVariableValue(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            variableNames = variableNames
        )
    }

    override fun executionPauseAtom(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskPauseExecute: BuildTaskPauseInfo
    ): Result<Boolean> {
        logger.info("OPENAPI_BUILD_V3|$userId|execution pause atom|$projectId|$pipelineId|$buildId|$taskPauseExecute")
        return client.get(ServiceBuildResource::class).executionPauseAtom(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskPauseExecute = taskPauseExecute
        )
    }

    override fun buildRestart(userId: String, projectId: String, pipelineId: String, buildId: String): Result<String> {
        logger.info("OPENAPI_BUILD_V3|$userId|build restart|$projectId|$pipelineId|$buildId")
        return client.get(ServiceBuildResource::class).buildRestart(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwBuildResourceV3Impl::class.java)
    }
}

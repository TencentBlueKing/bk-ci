/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineBuildQualityService
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.pipeline.AppModelDetail
import com.tencent.devops.process.service.app.AppBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppPipelineBuildResourceImpl @Autowired constructor(
    private val appBuildService: AppBuildService,
    private val buildService: PipelineBuildService,
    private val pipelineBuildQualityService: PipelineBuildQualityService
) : AppPipelineBuildResource {

    override fun manualQualityGateReview(
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
        pipelineBuildQualityService.buildManualQualityGateReview(
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
        buildService.buildManualReview(
            userId,
            projectId,
            pipelineId,
            buildId,
            elementId,
            params,
            ChannelCode.BS,
            ChannelCode.isNeedAuth(ChannelCode.BS)
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
            throw ParamBlankException("Invalid elementId")
        }

        return Result(buildService.goToReview(userId, projectId, pipelineId, buildId, elementId))
    }

    override fun manualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<BuildManualStartupInfo> {
        checkParam(userId, projectId, pipelineId)
        return Result(buildService.buildManualStartupInfo(userId, projectId, pipelineId, ChannelCode.BS))
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

    override fun getBuildDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<AppModelDetail> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(appBuildService.getBuildDetail(userId, projectId, pipelineId, buildId, ChannelCode.BS))
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
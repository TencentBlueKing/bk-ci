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

package com.tencent.devops.stream.resources.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.user.UserGitCIBuildResource
import com.tencent.devops.stream.constant.GitCIConstant.DEVOPS_PROJECT_PREFIX
import com.tencent.devops.stream.permission.GitCIV2PermissionService
import com.tencent.devops.stream.pojo.v2.GitCIV2Startup
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamTriggerService
import com.tencent.devops.process.pojo.BuildId
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGitCIBuildResourceImpl @Autowired constructor(
    private val streamTriggerService: StreamTriggerService,
    private val permissionService: GitCIV2PermissionService
) : UserGitCIBuildResource {

    override fun retry(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String?,
        failedContainer: Boolean?
    ): Result<BuildId> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId, pipelineId, buildId)
        permissionService.checkGitCIAndOAuthAndEnable(userId, projectId, gitProjectId)
        return Result(
            streamTriggerService.retry(
                userId = userId,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId,
                failedContainer = failedContainer
            )
        )
    }

    override fun manualShutdown(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId, pipelineId, buildId)
        permissionService.checkGitCIAndOAuthAndEnable(userId, projectId, gitProjectId)
        return Result(
            streamTriggerService.manualShutdown(
                userId = userId,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
        )
    }

    override fun gitCIStartupPipeline(userId: String, gitCIV2Startup: GitCIV2Startup): Result<BuildId?> {
        permissionService.checkGitCIAndOAuthAndEnable(
            userId,
            "$DEVOPS_PROJECT_PREFIX${gitCIV2Startup.gitCIBasicSetting.gitProjectId}",
            gitCIV2Startup.gitCIBasicSetting.gitProjectId
        )
        return Result(
            streamTriggerService.startBuild(
                pipeline = gitCIV2Startup.pipeline,
                event = gitCIV2Startup.event,
                gitCIBasicSetting = gitCIV2Startup.gitCIBasicSetting,
                model = gitCIV2Startup.model,
                gitBuildId = gitCIV2Startup.gitBuildId
            )
        )
    }

    private fun checkParam(userId: String, pipelineId: String, buildId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
    }
}

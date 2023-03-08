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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentBuildDetail
import com.tencent.devops.process.engine.pojo.event.PipelineStreamEnabledEvent
import com.tencent.devops.repository.pojo.AppInstallationResult
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.stream.api.user.UserGitBasicSettingResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.StreamUpdateSetting
import com.tencent.devops.stream.pojo.TriggerReviewSetting
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.service.StreamGitTransferService
import com.tencent.devops.stream.util.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGitBasicSettingResourceImpl @Autowired constructor(
    private val streamBasicSettingService: StreamBasicSettingService,
    private val permissionService: StreamPermissionService,
    private val streamGitTransferService: StreamGitTransferService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val streamGitConfig: StreamGitConfig
) : UserGitBasicSettingResource {

    @BkTimed
    override fun enableStream(
        userId: String,
        enabled: Boolean,
        projectInfo: StreamGitProjectInfoWithProject
    ): Result<Boolean> {
        val projectId = GitCommonUtils.getCiProjectId(projectInfo.gitProjectId, streamGitConfig.getScmType())
        val gitProjectId = projectInfo.gitProjectId
        checkParam(userId)
        permissionService.checkCommonUser(userId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT
        )
        val setting = streamBasicSettingService.getStreamConf(gitProjectId)
        val result = if (setting == null) {
            streamBasicSettingService.initStreamConf(
                userId = userId,
                projectId = projectId,
                gitProjectId = gitProjectId,
                enabled = enabled
            )
        } else {
            streamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                userId = userId,
                enableCi = enabled
            )
        }
        pipelineEventDispatcher.dispatch(
            PipelineStreamEnabledEvent(
                source = "stream_enabled",
                projectId = projectId,
                pipelineId = "",
                userId = userId,
                gitProjectId = projectInfo.gitProjectId,
                gitProjectUrl = projectInfo.gitHttpsUrl ?: "",
                enable = enabled
            )
        )
        return Result(result)
    }

    override fun getStreamConf(userId: String, projectId: String): Result<StreamBasicSetting?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        return Result(streamBasicSettingService.getStreamConf(gitProjectId))
    }

    override fun saveStreamConf(
        userId: String,
        projectId: String,
        streamUpdateSetting: StreamUpdateSetting
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamAndOAuthAndEnable(
            userId = userId,
            projectId = projectId,
            gitProjectId = gitProjectId,
            permission = AuthPermission.EDIT
        )
        return Result(
            streamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                buildPushedPullRequest = streamUpdateSetting.buildPushedPullRequest,
                buildPushedBranches = streamUpdateSetting.buildPushedBranches,
                enableMrBlock = streamUpdateSetting.enableMrBlock
            )
        )
    }

    override fun saveStreamReviewSetting(
        userId: String,
        projectId: String,
        triggerReviewSetting: TriggerReviewSetting
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamAndOAuthAndEnable(
            userId = userId,
            projectId = projectId,
            gitProjectId = gitProjectId,
            permission = AuthPermission.EDIT
        )
        return Result(
            streamBasicSettingService.updateProjectReviewSetting(
                gitProjectId = gitProjectId,
                triggerReviewSetting = triggerReviewSetting
            )
        )
    }

    override fun updateEnableUser(
        userId: String,
        projectId: String,
        authUserId: String
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkCommonUser(userId)
        permissionService.checkStreamAndOAuthAndEnable(
            userId = userId,
            projectId = projectId,
            gitProjectId = gitProjectId,
            permission = AuthPermission.EDIT
        )
        permissionService.checkStreamAndOAuthAndEnable(
            userId = authUserId,
            projectId = projectId,
            gitProjectId = gitProjectId,
            permission = AuthPermission.EDIT
        )
        return Result(
            streamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                userId = userId,
                authUserId = authUserId
            )
        )
    }

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        val projectId = GitCommonUtils.getCiProjectId(gitProjectId, streamGitConfig.getScmType())
        checkParam(userId)
        permissionService.checkCommonUser(userId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT
        )
        return streamGitTransferService.isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            gitProjectId = gitProjectId,
            refreshToken = refreshToken
        )
    }

    override fun listAgentBuilds(
        userId: String,
        projectId: String,
        nodeHashId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>> {
        checkParam(userId)
        checkProjectId(projectId)
        checkNodeId(nodeHashId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        return Result(streamBasicSettingService.listAgentBuilds(userId, projectId, nodeHashId, page, pageSize))
    }

    override fun isInstallApp(userId: String, gitProjectId: Long): Result<AppInstallationResult> {
        return Result(streamGitTransferService.isInstallApp(userId = userId, gitProjectId = gitProjectId))
    }

    private fun checkNodeId(nodeHashId: String) {
        if (nodeHashId.isBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("nodeId"))
        }
    }

    private fun checkProjectId(projectId: String) {
        if (projectId.isBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("projectId"))
        }
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}

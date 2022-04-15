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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentBuildDetail
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.stream.api.user.UserGitBasicSettingResource
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.constant.StreamConstant.DEVOPS_PROJECT_PREFIX
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.StreamUpdateSetting
import com.tencent.devops.stream.service.StreamGitTransferService
import com.tencent.devops.stream.service.TXStreamBasicSettingService
import com.tencent.devops.stream.util.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary

@Primary
@RestResource
class TXUserGitBasicSettingResourceImpl @Autowired constructor(
    private val client: Client,
    private val TXStreamBasicSettingService: TXStreamBasicSettingService,
    private val permissionService: StreamPermissionService,
    private val streamGitTransferService: StreamGitTransferService,
    private val userGitBasicSettingResourceImpl: UserGitBasicSettingResourceImpl
) : UserGitBasicSettingResource {

    override fun enableStream(
        userId: String,
        enabled: Boolean,
        projectInfo: StreamGitProjectInfoWithProject
    ): Result<Boolean> {
        val projectId = "$DEVOPS_PROJECT_PREFIX${projectInfo.gitProjectId}"
        val gitProjectId = projectInfo.gitProjectId
        checkParam(userId)
        checkCommonUser(userId)
        permissionService.checkStreamAndOAuth(
            userId = userId,
            projectId = projectId
        )
        val setting = TXStreamBasicSettingService.getStreamConf(gitProjectId)
        val result = if (setting == null) {
            TXStreamBasicSettingService.initStreamConf(userId, projectId, gitProjectId, enabled)
        } else {
            TXStreamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                enableCi = enabled
            )
        }
        return Result(result)
    }

    override fun getStreamConf(userId: String, projectId: String): Result<StreamBasicSetting?> {
        return userGitBasicSettingResourceImpl.getStreamConf(userId, projectId)
    }

    override fun saveStreamConf(
        userId: String,
        projectId: String,
        streamUpdateSetting: StreamUpdateSetting
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamPermission(userId = userId, projectId = projectId)
        permissionService.checkEnableStream(gitProjectId)
        return Result(
            TXStreamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                buildPushedPullRequest = streamUpdateSetting.buildPushedPullRequest,
                buildPushedBranches = streamUpdateSetting.buildPushedBranches,
                enableMrBlock = streamUpdateSetting.enableMrBlock
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
        checkCommonUser(userId)
        permissionService.checkStreamAndOAuthAndEnable(userId, projectId, gitProjectId)
        permissionService.checkStreamAndOAuthAndEnable(authUserId, projectId, gitProjectId)
        return Result(
            TXStreamBasicSettingService.updateProjectSetting(
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
        gitProjectId: Long?,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        return userGitBasicSettingResourceImpl.isOAuth(userId, redirectUrlType, redirectUrl, gitProjectId, refreshToken)
    }

    override fun listAgentBuilds(
        userId: String,
        projectId: String,
        nodeHashId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>> {
        return userGitBasicSettingResourceImpl.listAgentBuilds(userId, projectId, nodeHashId, page, pageSize)
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    // 判断用户是否公共账号，并且存在，否则提示用户注册
    private fun checkCommonUser(userId: String) {
        // get接口先查本地，再查tof
        val userResult =
            client.get(ServiceTxUserResource::class).get(userId)
        if (userResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.COMMON_USER_NOT_EXISTS.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.COMMON_USER_NOT_EXISTS.formatErrorMessage.format(userId)
            )
        }
    }
}

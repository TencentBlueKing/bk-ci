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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceGitOauthResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentBuildDetail
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.stream.constant.GitCIConstant.DEVOPS_PROJECT_PREFIX
import com.tencent.devops.stream.permission.GitCIV2PermissionService
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.pojo.v2.GitCIUpdateSetting
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.TXStreamBasicSettingService
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class TXUserGitBasicSettingResourceImpl @Autowired constructor(
    private val TXStreamBasicSettingService: TXStreamBasicSettingService,
    private val permissionService: GitCIV2PermissionService,
    private val client: Client
) : UserGitBasicSettingResourceImpl(
    TXStreamBasicSettingService,
    permissionService,
    client
) {

    override fun enableGitCI(
        userId: String,
        enabled: Boolean,
        projectInfo: GitCIProjectInfo
    ): Result<Boolean> {
        val projectId = "$DEVOPS_PROJECT_PREFIX${projectInfo.gitProjectId}"
        val gitProjectId = projectInfo.gitProjectId
        checkParam(userId)
        checkCommonUser(userId)
        permissionService.checkGitCIAndOAuth(
            userId = userId,
            projectId = projectId
        )
        val setting = TXStreamBasicSettingService.getGitCIConf(gitProjectId)
        val result = if (setting == null) {
            TXStreamBasicSettingService.initGitCIConf(userId, projectId, gitProjectId, enabled)
        } else {
            TXStreamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                enableCi = enabled
            )
        }
        return Result(result)
    }

    override fun getGitCIConf(userId: String, projectId: String): Result<GitCIBasicSetting?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkGitCIPermission(userId, projectId)
        return Result(TXStreamBasicSettingService.getGitCIConf(gitProjectId))
    }

    override fun saveGitCIConf(
        userId: String,
        projectId: String,
        gitCIUpdateSetting: GitCIUpdateSetting
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkGitCIPermission(userId = userId, projectId = projectId)
        permissionService.checkEnableGitCI(gitProjectId)
        return Result(
            TXStreamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                buildPushedPullRequest = gitCIUpdateSetting.buildPushedPullRequest,
                buildPushedBranches = gitCIUpdateSetting.buildPushedBranches,
                enableMrBlock = gitCIUpdateSetting.enableMrBlock
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
        permissionService.checkGitCIAndOAuthAndEnable(userId, projectId, gitProjectId)
        permissionService.checkGitCIAndOAuthAndEnable(authUserId, projectId, gitProjectId)
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
        return client.get(ServiceGitOauthResource::class).isOAuth(
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
        return Result(TXStreamBasicSettingService.listAgentBuilds(userId, projectId, nodeHashId, page, pageSize))
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

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

package com.tencent.devops.stream.resources.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.service.ServiceGitBasicSettingResource
import com.tencent.devops.stream.constant.GitCIConstant.DEVOPS_PROJECT_PREFIX
import com.tencent.devops.stream.permission.GitCIV2PermissionService
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.pojo.v2.GitCIUpdateSetting
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamBasicSettingService
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.pojo.v2.GitUserValidateRequest
import com.tencent.devops.stream.pojo.v2.GitUserValidateResult
import com.tencent.devops.stream.v2.service.StreamScmService
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGitBasicSettingResourceImpl @Autowired constructor(
    private val streamBasicSettingService: StreamBasicSettingService,
    private val permissionService: GitCIV2PermissionService,
    private val streamScmService: StreamScmService
) : ServiceGitBasicSettingResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceGitBasicSettingResourceImpl::class.java)
    }

    override fun enableGitCI(
        userId: String,
        enabled: Boolean,
        projectInfo: GitCIProjectInfo
    ): Result<Boolean> {
        val projectId = "$DEVOPS_PROJECT_PREFIX${projectInfo.gitProjectId}"
        val gitProjectId = projectInfo.gitProjectId.toLong()
        checkParam(userId)
        val setting = streamBasicSettingService.getGitCIConf(gitProjectId)
        val result = if (setting == null) {
            streamBasicSettingService.initGitCIConf(userId, projectId, gitProjectId, enabled, projectInfo)
        } else {
            streamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                enableCi = enabled
            )
        }
        return Result(result)
    }

    override fun getGitCIConf(userId: String, projectId: String): Result<GitCIBasicSetting?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        return Result(streamBasicSettingService.getGitCIConf(gitProjectId))
    }

    override fun validateGitProject(
        userId: String,
        request: GitUserValidateRequest
    ): Result<GitUserValidateResult> {
        logger.info("STREAM|validateGitProject|request=$request")
        val projectName = GitUtils.getProjectName(request.url)
        // 直接请求新的token，如果不是合法的项目在获取时直接报错
        val token = streamScmService.getToken(projectName).accessToken
        val projectInfo = streamScmService.getProjectInfo(
            gitProjectId = projectName,
            token = token,
            useAccessToken = true
        ) ?: throw CustomException(
            status = Response.Status.NOT_FOUND,
            message = "工蜂项目无法找到，请检查链接"
        )
        logger.info("STREAM|validateGitProjectInfo|projectInfo=$projectInfo")
        val gitProjectId = projectInfo.gitProjectId.toLong()
        val projectCode = GitCommonUtils.getCiProjectId(gitProjectId)
        permissionService.checkGitCIPermission(userId, projectCode, AuthPermission.EDIT)
        val setting = streamBasicSettingService.getGitCIConf(gitProjectId)
            ?: throw CustomException(
                status = Response.Status.NOT_FOUND,
                message = "工蜂项目未注册，请检查链接"
            )
        logger.info("STREAM|validateGitProjectSetting|setting=$setting")
        return Result(GitUserValidateResult(
            gitProjectId = gitProjectId,
            name = setting.name,
            url = setting.url,
            homepage = setting.homepage,
            gitHttpUrl = setting.gitHttpUrl,
            gitSshUrl = setting.gitSshUrl,
            projectCode = projectCode,
            projectName = projectInfo.nameWithNamespace,
            enableCi = setting.enableCi
        ))
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
            streamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                buildPushedPullRequest = gitCIUpdateSetting.buildPushedPullRequest,
                buildPushedBranches = gitCIUpdateSetting.buildPushedBranches,
                enableMrBlock = gitCIUpdateSetting.enableMrBlock
            )
        )
    }

    override fun updateEnableUser(userId: String, projectId: String): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkGitCIAndOAuthAndEnable(userId, projectId, gitProjectId)
        return Result(
            streamBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                enableUserId = userId
            )
        )
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}

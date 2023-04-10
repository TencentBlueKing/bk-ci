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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.pojo.event.PipelineStreamEnabledEvent
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.store.constant.StoreMessageCode.BK_WORKER_BEE_PROJECT_NOT_EXIST
import com.tencent.devops.store.constant.StoreMessageCode.BK_WORKER_BEE_PROJECT_NOT_STREAM_ENABLED
import com.tencent.devops.stream.api.service.ServiceGitBasicSettingResource
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.openapi.GitCIBasicSetting
import com.tencent.devops.stream.pojo.openapi.GitCIUpdateSetting
import com.tencent.devops.stream.pojo.openapi.GitUserValidateRequest
import com.tencent.devops.stream.pojo.openapi.GitUserValidateResult
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.service.TXStreamBasicSettingService
import com.tencent.devops.stream.util.GitCommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class ServiceGitBasicSettingResourceImpl @Autowired constructor(
    private val txStreamBasicSettingService: TXStreamBasicSettingService,
    private val permissionService: StreamPermissionService,
    private val streamScmService: StreamScmService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val client: Client,
    private val gitConfig: StreamGitConfig
) : ServiceGitBasicSettingResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceGitBasicSettingResourceImpl::class.java)
    }

    @BkTimed
    override fun enableGitCI(
        userId: String,
        enabled: Boolean,
        projectInfo: GitCIProjectInfo
    ): Result<Boolean> {
        val projectId = GitCommonUtils.getCiProjectId(projectInfo.gitProjectId, gitConfig.getScmType())
        val gitProjectId = projectInfo.gitProjectId
        checkParam(userId)
        checkCommonUser(userId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT
        )
        val setting = txStreamBasicSettingService.getStreamConf(gitProjectId)
        val result = if (setting == null) {
            txStreamBasicSettingService.initStreamConf(
                userId = userId,
                projectId = projectId,
                gitProjectId = gitProjectId,
                enabled = enabled
            )
        } else {
            txStreamBasicSettingService.updateProjectSetting(
                userId = userId,
                gitProjectId = gitProjectId,
                enableCi = enabled
            )
        }
        logger.info("dispatch stream enable event")
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

    override fun getGitCIConf(userId: String, projectId: String): Result<GitCIBasicSetting?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        return Result(txStreamBasicSettingService.getStreamConf(gitProjectId)?.let { GitCIBasicSetting(it) })
    }

    override fun validateGitProject(
        userId: String,
        request: GitUserValidateRequest
    ): Result<GitUserValidateResult?> {
        logger.info("STREAM|validateGitProject|request=$request")
        val projectName = try {
            GitUtils.getProjectName(request.url)
        } catch (t: Throwable) {
            return Result(
                status = Response.Status.BAD_REQUEST.statusCode,
                message = t.message,
                data = null
            )
        }
        // 直接请求新的token，如果不是合法的项目在获取时直接报错
        val token = try {
            streamScmService.getToken(projectName).accessToken
        } catch (t: Throwable) {
            return Result(
                status = Response.Status.BAD_REQUEST.statusCode,
                message = t.message,
                data = null
            )
        }
        val projectInfo = streamScmService.getProjectInfo(
            gitProjectId = projectName,
            token = token,
            useAccessToken = true
        ) ?: return Result(
            status = Response.Status.NOT_FOUND.statusCode,
            message = MessageUtil.getMessageByLocale(
                messageCode = BK_WORKER_BEE_PROJECT_NOT_EXIST,
                language = I18nUtil.getLanguage(userId)
            ),
            data = null
        )
        logger.info("STREAM|validateGitProjectInfo|projectInfo=$projectInfo")
        val gitProjectId = projectInfo.gitProjectId
        val projectCode = GitCommonUtils.getCiProjectId(gitProjectId, gitConfig.getScmType())

        permissionService.checkStreamPermission(userId, projectCode, AuthPermission.USE)

        val setting = txStreamBasicSettingService.getStreamConf(gitProjectId)
            ?: return Result(
                status = Response.Status.NOT_FOUND.statusCode,
                message = MessageUtil.getMessageByLocale(
                    messageCode = BK_WORKER_BEE_PROJECT_NOT_STREAM_ENABLED,
                    language = I18nUtil.getLanguage(userId)
                ),
                data = null
            )
        logger.info("STREAM|validateGitProjectSetting|setting=$setting")
        return Result(
            GitUserValidateResult(
                gitProjectId = gitProjectId,
                name = setting.name,
                url = setting.url,
                homepage = setting.homepage,
                gitHttpUrl = setting.gitHttpUrl,
                gitSshUrl = setting.gitSshUrl,
                projectCode = projectCode,
                projectName = projectInfo.nameWithNamespace,
                enableCi = setting.enableCi,
                authUserId = setting.enableUserId
            )
        )
    }

    override fun saveGitCIConf(
        userId: String,
        projectId: String,
        gitCIUpdateSetting: GitCIUpdateSetting
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamPermission(userId = userId, projectId = projectId)
        permissionService.checkEnableStream(gitProjectId)
        return Result(
            txStreamBasicSettingService.updateProjectSetting(
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
        permissionService.checkStreamAndOAuthAndEnable(authUserId, projectId, gitProjectId)
        return Result(
            txStreamBasicSettingService.updateProjectSetting(
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
        // 更改为每次都进行重定向授权
        return client.get(ServiceOauthResource::class).isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            gitProjectId = gitProjectId,
            refreshToken = true
        )
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    // 判断用户是否公共账号，并且存在，否则提示用户注册
    private fun checkCommonUser(userId: String) {
        // get接口先查本地，再查tof
        try {
            val userResult =
                client.get(ServiceTxUserResource::class).get(userId)
            if (userResult.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.COMMON_USER_NOT_EXISTS.errorCode.toString(),
                    defaultMessage = ErrorCodeEnum.COMMON_USER_NOT_EXISTS.formatErrorMessage.format(userId)
                )
            }
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.COMMON_USER_NOT_EXISTS.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.COMMON_USER_NOT_EXISTS.formatErrorMessage.format(userId)
            )
        }
    }
}

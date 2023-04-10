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

package com.tencent.devops.stream.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.stream.api.TXExternalStreamResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.BK_FAILED_VERIFY_AUTHORITY
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.service.StreamGitTransferService
import com.tencent.devops.stream.service.TXStreamBasicSettingService
import com.tencent.devops.stream.util.GitCommonUtils
import org.slf4j.LoggerFactory
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

@RestResource
class TXExternalStreamResourceImpl(
    private val basicSettingService: TXStreamBasicSettingService,
    private val client: Client,
    private val streamPermissionService: StreamPermissionService,
    private val streamGitConfig: StreamGitConfig,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val streamGitTransferService: StreamGitTransferService
) : TXExternalStreamResource {

    companion object {
        private val logger = LoggerFactory.getLogger(TXExternalStreamResourceImpl::class.java)
    }

    override fun gitCallback(code: String, state: String): Response {
        val gitOauthCallback = client.get(ServiceOauthResource::class).gitCallback(code = code, state = state).data!!
        with(gitOauthCallback) {
            logger.info("get oauth call back info: $gitOauthCallback")
            val gitProjectId = gitOauthCallback.gitProjectId
            if (gitProjectId != null) {
                kotlin.runCatching {
                    streamGitTransferService.enableCi(userId, gitProjectId.toString(), true)
                }.onFailure {
                    logger.warn("git call back enable ci failed|${it.message}")
                }

                val projectId = GitCommonUtils.getCiProjectId(gitProjectId, streamGitConfig.getScmType())
                try {
                    streamPermissionService.checkStreamPermission(
                        userId = userId,
                        projectId = projectId,
                        permission = AuthPermission.EDIT
                    )
                } catch (exception: Exception) {
                    return Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(Result(status = 403, message =
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_FAILED_VERIFY_AUTHORITY,
                            language = I18nUtil.getLanguage(userId)
                        ), data = exception.message)).build()
                }

                val setting = streamBasicSettingService.getStreamConf(gitProjectId)
                if (setting == null) {
                    streamBasicSettingService.initStreamConf(
                        userId = userId,
                        projectId = projectId,
                        gitProjectId = gitProjectId,
                        enabled = true
                    )
                } else {
                    streamBasicSettingService.updateProjectSetting(
                        gitProjectId = gitProjectId,
                        authUserId = oauthUserId,
                        userId = userId,
                        enableCi = true
                    )

                    // 更新项目信息
                    basicSettingService.updateProjectOrganizationInfo(
                        projectId = gitProjectId.toString(),
                        userId = oauthUserId
                    )
                }
            }
            return Response.temporaryRedirect(UriBuilder.fromUri(gitOauthCallback.redirectUrl).build()).build()
        }
    }
}

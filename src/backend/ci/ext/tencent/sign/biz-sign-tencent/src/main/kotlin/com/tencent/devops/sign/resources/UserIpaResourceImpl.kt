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

package com.tencent.devops.sign.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.sign.SignCode.BK_IOS_ENTERPRISE_RESIGNATURE
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.SignDetail
import com.tencent.devops.sign.api.user.UserIpaResource
import com.tencent.devops.sign.service.AsyncSignService
import com.tencent.devops.sign.service.DownloadService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
@Suppress("LongParameterList")
class UserIpaResourceImpl @Autowired constructor(
    private val signService: SignService,
    private val syncSignService: AsyncSignService,
    private val downloadService: DownloadService,
    private val signInfoService: SignInfoService,
    private val objectMapper: ObjectMapper,
    private val authPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode
) : UserIpaResource {

    override fun ipaSign(
        userId: String,
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ): Result<String?> {
        val resignId = "s-${UUIDUtil.generate()}"
        val ipaSignInfo = signInfoService.check(signInfoService.decodeIpaSignInfo(ipaSignInfoHeader, objectMapper))
        if (!checkParams(ipaSignInfo, userId)) {
            logger.warn(
                MessageUtil.getMessageByLocale(
                messageCode = BK_IOS_ENTERPRISE_RESIGNATURE,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(userId, ipaSignInfo.projectId, ipaSignInfo.pipelineId.toString())
            )

            )
            throw PermissionForbiddenException(
                message = MessageUtil.getMessageByLocale(
                    messageCode = BK_IOS_ENTERPRISE_RESIGNATURE,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(userId, ipaSignInfo.projectId, ipaSignInfo.pipelineId.toString())
                ))
        }
        var taskExecuteCount = 1
        try {
            val (ipaFile, taskExecuteCount2) =
                signService.uploadIpaAndDecodeInfo(resignId, ipaSignInfo, ipaSignInfoHeader, ipaInputStream)
            taskExecuteCount = taskExecuteCount2
            syncSignService.asyncSign(resignId, ipaSignInfo, ipaFile, taskExecuteCount)
            return Result(resignId)
        } catch (ignored: Exception) {
            signInfoService.failResign(
                resignId = resignId,
                info = ipaSignInfo,
                executeCount = taskExecuteCount,
                message = ignored.message ?: "Start sign task with exception"
            )
            throw ignored
        }
    }

    override fun getSignStatus(userId: String, resignId: String): Result<String> {
        return Result(signService.getSignStatus(resignId).getValue())
    }

    override fun getSignDetail(userId: String, resignId: String): Result<SignDetail> {
        return Result(signService.getSignDetail(resignId))
    }

    override fun downloadUrl(userId: String, resignId: String): Result<String> {
        return Result(downloadService.getDownloadUrl(
            userId = userId,
            resignId = resignId,
            downloadType = "user")
        )
    }

    private fun checkParams(
        ipaSignInfo: IpaSignInfo,
        userId: String
    ): Boolean {
        val projectId = ipaSignInfo.projectId
        val pipelineId = ipaSignInfo.pipelineId ?: ""
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            resourceCode = pipelineId,
            permission = AuthPermission.EXECUTE
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserIpaResourceImpl::class.java)
    }
}

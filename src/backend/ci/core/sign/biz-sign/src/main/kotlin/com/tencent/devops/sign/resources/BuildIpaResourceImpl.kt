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

package com.tencent.devops.sign.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.sign.api.builds.BuildIpaResource
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.SignDetail
import com.tencent.devops.sign.service.AsyncSignService
import com.tencent.devops.sign.service.DownloadService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class BuildIpaResourceImpl @Autowired constructor(
    private val signService: SignService,
    private val syncSignService: AsyncSignService,
    private val downloadService: DownloadService,
    private val signInfoService: SignInfoService,
    private val objectMapper: ObjectMapper
) : BuildIpaResource {
    companion object {
        val logger = LoggerFactory.getLogger(BuildIpaResourceImpl::class.java)
    }

    override fun ipaSign(
        projectId: String,
        pipelineId: String,
        buildId: String,
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ): Result<String> {
        val resignId = "s-${UUIDUtil.generate()}"
        val ipaSignInfo = signInfoService.check(signInfoService.decodeIpaSignInfo(ipaSignInfoHeader, objectMapper))
        if (!checkParams(ipaSignInfo, projectId, pipelineId, buildId)) {
            logger.warn("构建机无权限在工程(${ipaSignInfo.projectId})的流水线(${ipaSignInfo.pipelineId})中发起iOS企业重签名.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_NOT_AUTH_UPLOAD, defaultMessage = "构建机无权限在工程(${ipaSignInfo.projectId})的流水线(${ipaSignInfo.pipelineId})中发起iOS企业重签名.")
        }
        var taskExecuteCount = 1
        try {
            val (ipaFile, taskExecuteCount) =
                signService.uploadIpaAndDecodeInfo(resignId, ipaSignInfo, ipaSignInfoHeader, ipaInputStream)
            syncSignService.asyncSign(resignId, ipaSignInfo, ipaFile, taskExecuteCount)
            return Result(resignId)
        } catch (e: Exception) {
            signInfoService.failResign(
                resignId = resignId,
                info = ipaSignInfo,
                executeCount = taskExecuteCount,
                message = e.message ?: "Start sign task with exception"
            )
            throw e
        }
    }

    override fun getSignStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        resignId: String
    ): Result<String> {
        return Result(signService.getSignStatus(resignId).getValue())
    }

    override fun getSignDetail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        resignId: String
    ): Result<SignDetail> {
        return Result(signService.getSignDetail(resignId))
    }

    override fun downloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        resignId: String
    ): Result<String> {
        return Result(downloadService.getDownloadUrl(
            userId = "",
            resignId = resignId,
            downloadType = "build")
        )
    }

    private fun checkParams(
        ipaSignInfo: IpaSignInfo,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Boolean {
        return ipaSignInfo.projectId == projectId && ipaSignInfo.pipelineId == pipelineId && ipaSignInfo.buildId == buildId
    }
}
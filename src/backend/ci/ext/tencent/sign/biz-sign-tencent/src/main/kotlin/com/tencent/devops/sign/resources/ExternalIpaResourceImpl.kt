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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.external.ExternalIpaResource
import com.tencent.devops.sign.dao.IpaUploadDao
import com.tencent.devops.sign.service.AsyncSignService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.io.InputStream

@RestResource
@Suppress("ALL")
class ExternalIpaResourceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val ipaUploadDao: IpaUploadDao,
    private val signService: SignService,
    private val syncSignService: AsyncSignService,
    private val signInfoService: SignInfoService,
    private val objectMapper: ObjectMapper
) : ExternalIpaResource {

    @Value("\${bkci.sign.tokenExpiresInMinutes:120}")
    private val tokenExpiresInMinutes: Int = 120

    override fun ipaUpload(ipaSignInfoHeader: String, ipaInputStream: InputStream, token: String): Result<String> {
        val resignId = "s-${UUIDUtil.generate()}"
        val ipaSignInfo = signInfoService.check(signInfoService.decodeIpaSignInfo(ipaSignInfoHeader, objectMapper))
        val uploadRecord = ipaUploadDao.get(dslContext, token)
            ?: throw ErrorCodeException(
                errorCode = SignMessageCode.ERROR_UPLOAD_TOKEN_INVALID,
                defaultMessage = "使用的上传token无效"
            )

        // 判断token是否过期
        if (System.currentTimeMillis() - uploadRecord.createTime.timestampmilli() > tokenExpiresInMinutes * 60 * 1000 ||
            !uploadRecord.resignId.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = SignMessageCode.ERROR_UPLOAD_TOKEN_EXPIRED,
                defaultMessage = "使用的上传token已过期"
            )
        }
        // 对签名信息做替换
        ipaSignInfo.projectId = uploadRecord.projectId
        ipaSignInfo.pipelineId = uploadRecord.pipelineId
        ipaSignInfo.buildId = uploadRecord.buildId

        var taskExecuteCount = 1
        try {
            val (ipaFile, taskExecuteCount2) = signService.uploadIpaAndDecodeInfo(
                resignId = resignId,
                ipaSignInfo = ipaSignInfo,
                ipaSignInfoHeader = ipaSignInfoHeader,
                ipaInputStream = ipaInputStream,
                md5Check = false
            )
            taskExecuteCount = taskExecuteCount2
            ipaUploadDao.update(dslContext, token, resignId)
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
}

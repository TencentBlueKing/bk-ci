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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.sign.api.pojo.IpaUploadInfo
import com.tencent.devops.sign.api.pojo.SignDetail
import com.tencent.devops.sign.api.pojo.SignHistory
import com.tencent.devops.sign.api.service.ServiceIpaResource
import com.tencent.devops.sign.dao.IpaUploadDao
import com.tencent.devops.sign.service.AsyncSignService
import com.tencent.devops.sign.service.DownloadService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
@Suppress("ALL")
class ServiceIpaResourceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val ipaUploadDao: IpaUploadDao,
    private val signService: SignService,
    private val syncSignService: AsyncSignService,
    private val downloadService: DownloadService,
    private val signInfoService: SignInfoService,
    private val objectMapper: ObjectMapper
) : ServiceIpaResource {

    override fun getHistorySign(
        userId: String,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<SignHistory>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize?.coerceAtMost(20) ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = signInfoService.listHistory(
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            offset = limit.offset,
            limit = limit.limit
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun ipaSign(
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream,
        md5Check: Boolean
    ): Result<String> {
        val resignId = "s-${UUIDUtil.generate()}"
        val ipaSignInfo = signInfoService.check(signInfoService.decodeIpaSignInfo(ipaSignInfoHeader, objectMapper))
        var taskExecuteCount = 1
        try {
            val (ipaFile, taskExecuteCount2) = signService.uploadIpaAndDecodeInfo(
                resignId = resignId,
                ipaSignInfo = ipaSignInfo,
                ipaSignInfoHeader = ipaSignInfoHeader,
                ipaInputStream = ipaInputStream,
                md5Check = md5Check
            )
            taskExecuteCount = taskExecuteCount2
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

    override fun getSignToken(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<IpaUploadInfo> {
        val token = UUIDUtil.generate()
        ipaUploadDao.save(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            uploadToken = token
        )
        return Result(IpaUploadInfo(projectId, pipelineId, buildId, token))
    }

    override fun getSignStatus(resignId: String): Result<String> {
        return Result(signService.getSignStatus(resignId).getValue())
    }

    override fun getSignDetail(resignId: String): Result<SignDetail> {
        return Result(signService.getSignDetail(resignId))
    }

    override fun downloadUrl(resignId: String): Result<String> {
        return Result(downloadService.getDownloadUrl(
            userId = "",
            resignId = resignId,
            downloadType = "service")
        )
    }
}

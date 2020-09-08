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

package com.tencent.devops.sign.service.impl

import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.service.config.CommonConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.service.DownloadService
import org.jooq.DSLContext
import java.net.URLEncoder

@Service
class DownloadServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao,
    private val signHistoryDao: SignHistoryDao,
    private val commonConfig: CommonConfig
) : DownloadService {

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadServiceImpl::class.java)
    }

    override fun getDownloadUrl(userId: String, resignId: String, downloadType: String): String {
        val signIpaInfoResult = signIpaInfoDao.getSignInfo(dslContext, resignId)
        if (signIpaInfoResult == null) {
            logger.error("签名任务签名信息(resignId=$resignId)不存在。")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_RESIGN_TASK_NOT_EXIST, defaultMessage = "签名任务不存在。")
        }
        val signHistoryResult = signHistoryDao.getSignHistory(dslContext, resignId)
        if (signHistoryResult == null) {
            logger.error("签名任务签名历史(resignId=$resignId)不存在。")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_RESIGN_TASK_NOT_EXIST, defaultMessage = "签名任务不存在。")
        }
        val filePath = when (signIpaInfoResult.archiveType.toLowerCase()) {
            "pipeline" -> {
                "${FileTypeEnum.BK_ARCHIVE.fileType}/${signIpaInfoResult.projectId}/${signIpaInfoResult.pipelineId}/${signIpaInfoResult.buildId}/${signHistoryResult.resultFileName ?: "result.ipa"}"
            }
            "custom" -> {
                "${FileTypeEnum.BK_CUSTOM.fileType}/${signIpaInfoResult.projectId}/${signIpaInfoResult.archivePath?.trim('/')}/${signHistoryResult.resultFileName ?: "result.ipa"}"
            }
            else -> {
                // 默认是流水线
                "${FileTypeEnum.BK_ARCHIVE.fileType}/${signIpaInfoResult.projectId}/${signIpaInfoResult.pipelineId}/${signIpaInfoResult.buildId}/${signHistoryResult.resultFileName ?: "result.ipa"}"
            }
        }
        val downloadTypePath = when (downloadType) {
            "user" -> "user"
            "service" -> "service"
            "build" -> "build"
            else -> "user"
        }

        return "${commonConfig.devopsHostGateway}/artifactory/api/$downloadTypePath/artifactories/file/download/local?filePath=${URLEncoder.encode(filePath, "UTF-8")}"
    }
}
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

package com.tencent.devops.sign.service.impl

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.pojo.enums.GatewayType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.sign.SignCode.BK_FAILED_CREATE_DOWNLOAD_CONNECTION
import com.tencent.devops.sign.SignCode.BK_SIGNING_TASK_SIGNATURE_HISTORY
import com.tencent.devops.sign.SignCode.BK_SIGNING_TASK_SIGNATURE_INFORMATION
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.service.DownloadService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BsDownloadServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao,
    private val signHistoryDao: SignHistoryDao,
    private val commonConfig: CommonConfig,
    private val client: Client
) : DownloadService {

    companion object {
        private val logger = LoggerFactory.getLogger(BsDownloadServiceImpl::class.java)
    }

    override fun getDownloadUrl(userId: String, resignId: String, downloadType: String): String {
        val signIpaInfoResult = signIpaInfoDao.getSignInfo(dslContext, resignId)
        if (signIpaInfoResult == null) {
            logger.warn(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_SIGNING_TASK_SIGNATURE_INFORMATION,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(resignId)
                )
            )
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_RESIGN_TASK_NOT_EXIST, defaultMessage = "签名任务不存在。")
        }
        val signHistoryResult = signHistoryDao.getSignHistory(dslContext, resignId)
        if (signHistoryResult == null) {
            logger.warn(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_SIGNING_TASK_SIGNATURE_HISTORY,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(resignId)
                )
            )
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_RESIGN_TASK_NOT_EXIST, defaultMessage = "签名任务不存在。")
        }
        var artifactoryType: ArtifactoryType? = null
        var path: String? = null
        when (signIpaInfoResult.archiveType.toLowerCase()) {
            "pipeline" -> {
                artifactoryType = ArtifactoryType.PIPELINE
                path = "/${signIpaInfoResult.pipelineId}/${signIpaInfoResult.buildId}/${signHistoryResult.resultFileName}"
            }
            "custom" -> {
                artifactoryType = ArtifactoryType.CUSTOM_DIR
                path = "/${signIpaInfoResult.archivePath?.trim('/')}/${signHistoryResult.resultFileName}"
            }
            else -> {
                artifactoryType = ArtifactoryType.PIPELINE
                path = "/${signIpaInfoResult.pipelineId}/${signIpaInfoResult.buildId}/${signHistoryResult.resultFileName}"
            }
        }
        var downlouadUserId = when (downloadType) {
            "service", "build" -> {
                signIpaInfoResult.userId
            }
            "user" -> {
                userId
            }
            else -> {
                userId
            }
        }
        val downloadUrl = client.getGateway(ServiceArtifactoryDownLoadResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
            projectId = signIpaInfoResult.projectId,
            artifactoryType = artifactoryType,
            userId = downlouadUserId,
            path = path,
            ttl = 7200,
            directed = true

        ).data?.url
//        val downloadUrl = when (downloadType) {
//            "service","build" -> {
//                client.getGateway(ServiceArtifactoryDownLoadResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
//                        projectId = signIpaInfoResult.projectId,
//                        artifactoryType = artifactoryType,
//                        userId = signIpaInfoResult.userId,
//                        path = path,
//                        ttl = 7200,
//                        directed = true
//
//                ).data?.url
//            }
//            "user" -> {
//                client.getGateway(UserArtifactoryResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
//                        userId = userId,
//                        projectId = signIpaInfoResult.projectId,
//                        artifactoryType = artifactoryType,
//                        path = path
//                ).data?.url
//            }
//            else -> {
//                client.getGateway(UserArtifactoryResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
//                        userId = userId,
//                        projectId = signIpaInfoResult.projectId,
//                        artifactoryType = artifactoryType,
//                        path = path
//                ).data?.url
//            }
//        }
        if (downloadUrl == null) {
            logger.error(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_FAILED_CREATE_DOWNLOAD_CONNECTION,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(resignId)
                )
            )
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_CREATE_DOWNLOAD_URL, defaultMessage = "创建下载连接失败。")
        }
        return downloadUrl
    }
}

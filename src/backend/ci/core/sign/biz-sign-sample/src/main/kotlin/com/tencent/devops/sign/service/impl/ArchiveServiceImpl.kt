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

import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.ArchiveService
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.net.SocketTimeoutException

@Service
class ArchiveServiceImpl @Autowired constructor(
    private val commonConfig: CommonConfig
) : ArchiveService {

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveServiceImpl::class.java)
    }

    override fun archive(
        signedIpaFile: File,
        ipaSignInfo: IpaSignInfo,
        properties: MutableMap<String, String>?
    ): Boolean {
        logger.info(
            "uploadFile, userId: ${ipaSignInfo.userId}, projectId: ${ipaSignInfo.projectId}," +
                "archiveType: ${ipaSignInfo.archiveType}, archivePath: ${ipaSignInfo.archivePath}"
        )
        val artifactoryType = when (ipaSignInfo.archiveType.toLowerCase()) {
            "pipeline" -> FileTypeEnum.BK_ARCHIVE
            "custom" -> FileTypeEnum.BK_CUSTOM
            else -> FileTypeEnum.BK_ARCHIVE
        }
        val url =
            "${commonConfig.devopsDevnetProxyGateway}/ms/artifactory/api/service/artifactories/file/archive" +
                "?fileType=$artifactoryType&customFilePath=${ipaSignInfo.archivePath}"
        val fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), signedIpaFile)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", signedIpaFile.name, fileBody)
            .build()
        val headers = mutableMapOf<String, String>()
        headers[AUTH_HEADER_DEVOPS_PROJECT_ID] = ipaSignInfo.projectId
        headers[AUTH_HEADER_DEVOPS_PIPELINE_ID] = ipaSignInfo.pipelineId ?: ""
        headers[AUTH_HEADER_DEVOPS_BUILD_ID] = ipaSignInfo.buildId ?: ""
        try {
            val request = Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .post(requestBody)
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("artifactory upload file failed. url:$url. response:$responseContent")
                    return false
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("artifactory upload file with timeout, need retry", e)
            throw e
        } catch (ignore: Throwable) {
            logger.error("artifactory upload file with error. url:$url", ignore)
            return false
        }
        return true
    }
}

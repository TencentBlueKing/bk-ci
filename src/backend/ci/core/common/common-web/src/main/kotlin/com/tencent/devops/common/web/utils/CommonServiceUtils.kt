/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.web.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_JWT_TOKEN
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.security.jwt.JwtManager
import com.tencent.devops.common.service.utils.SpringContextUtil
import java.io.File
import okhttp3.Response
import org.slf4j.LoggerFactory

object CommonServiceUtils {
    private val logger = LoggerFactory.getLogger(CommonServiceUtils::class.java)

    fun uploadFileToArtifactories(
        userId: String,
        serviceUrlPrefix: String,
        file: File,
        fileChannelType: String,
        staticFlag: Boolean = false,
        language: String,
        fileRepoPath: String? = null
    ): Result<String?> {
        var serviceUrl = "$serviceUrlPrefix/service/artifactories/file/upload" +
            "?userId=$userId&fileChannelType=$fileChannelType&staticFlag=$staticFlag"
        fileRepoPath?.let {
            serviceUrl += "&filePath=$fileRepoPath"
        }
        logger.info("serviceUploadFile serviceUrl is:$serviceUrl")
        uploadFileToService(serviceUrl, file).use { response ->
            val responseContent = response.body!!.string()
            logger.error("uploadFile responseContent is: $responseContent")
            if (!response.isSuccessful) {
                val message = MessageUtil.getMessageByLocale(
                    messageCode = CommonMessageCode.SYSTEM_ERROR,
                    language = language
                )
                Result(CommonMessageCode.SYSTEM_ERROR.toInt(), message, null)
            }
            return JsonUtil.to(responseContent, object : TypeReference<Result<String?>>() {})
        }
    }

    fun uploadFileToService(
        url: String,
        uploadFile: File,
        headers: MutableMap<String, String> = mutableMapOf(),
        fileFieldName: String = "file",
        fileName: String = uploadFile.name
    ): Response {
        val jwtManager = SpringContextUtil.getBean(JwtManager::class.java)
        if (jwtManager.isSendEnable()) {
            val jwtToken = jwtManager.getToken() ?: ""
            headers[AUTH_HEADER_DEVOPS_JWT_TOKEN] = jwtToken
        }
        return OkhttpUtils.uploadFile(
            url = url,
            uploadFile = uploadFile,
            headers = headers,
            fileFieldName = fileFieldName,
            fileName = fileName
        )
    }

    fun doPostFromService(
        url: String,
        jsonParam: String,
        headers: MutableMap<String, String> = mutableMapOf()
    ): Response {
        val jwtManager = SpringContextUtil.getBean(JwtManager::class.java)
        if (jwtManager.isSendEnable()) {
            val jwtToken = jwtManager.getToken() ?: ""
            headers[AUTH_HEADER_DEVOPS_JWT_TOKEN] = jwtToken
        }
        return OkhttpUtils.doPost(url, jsonParam, headers)
    }

    fun downloadFileFromService(
        url: String,
        destPath: File,
        headers: MutableMap<String, String> = mutableMapOf(),
        connectTimeoutInSec: Long? = null,
        readTimeoutInSec: Long? = null,
        writeTimeoutInSec: Long? = null
    ) {
        val jwtManager = SpringContextUtil.getBean(JwtManager::class.java)
        if (jwtManager.isSendEnable()) {
            val jwtToken = jwtManager.getToken() ?: ""
            headers[AUTH_HEADER_DEVOPS_JWT_TOKEN] = jwtToken
        }
        OkhttpUtils.downloadFile(
            url = url,
            destPath = destPath,
            headers = headers,
            connectTimeoutInSec = connectTimeoutInSec,
            readTimeoutInSec = readTimeoutInSec,
            writeTimeoutInSec = writeTimeoutInSec
        )
    }
}

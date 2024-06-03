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

package com.tencent.devops.worker.common.api.archive

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.artifactory.constant.REALM_LOCAL
import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_DOWNLOAD_LINK_REQUEST_ERROR
import com.tencent.devops.worker.common.constants.WorkerMessageCode.UPLOAD_CUSTOM_FILE_FAILED
import com.tencent.devops.worker.common.constants.WorkerMessageCode.UPLOAD_PIPELINE_FILE_FAILED
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.net.URLEncoder

@Suppress("UNUSED")
class ArchiveResourceApi : AbstractBuildResourceApi(), ArchiveSDKApi {

    override fun getRealm(): String {
        return REALM_LOCAL
    }

    override fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?
    ): List<String> {
        val purePath = if (customFilePath != null) {
            purePath(customFilePath)
        } else {
            customFilePath
        }

        val url = "/ms/artifactory/api/build/artifactories/pipeline/$pipelineId/build/$buildId/file/download/urls/get" +
                "?fileType=$fileType&customFilePath=$purePath"
        val request = buildGet(url)
        val message = MessageUtil.getMessageByLocale(
            GET_DOWNLOAD_LINK_REQUEST_ERROR, AgentEnv.getLocaleLanguage()
        )
        val response = request(request, message)
        val result = try {
            objectMapper.readValue<Result<GetFileDownloadUrlsResponse?>>(response)
        } catch (ignored: Exception) {
            LoggerService.addNormalLine(ignored.message ?: "")
            throw RemoteServiceException("archive fail: $response")
        }
        if (result.isNotOk()) {
            throw RemoteServiceException(result.message ?: "$message，${result.status.toLong()}")
        }

        return result.data?.fileUrlList ?: emptyList()
    }

    override fun uploadCustomize(file: File, destPath: String, buildVariables: BuildVariables, token: String?) {
        // 过滤掉用../尝试遍历上层目录的操作
        val purePath = purePath(destPath)
        val path = purePath + "/" + file.name
        LoggerService.addNormalLine("upload file >>> $path")

        val url = "/ms/artifactory/api/build/artifactories/file/archive" +
                "?fileType=${FileTypeEnum.BK_CUSTOM}&customFilePath=$purePath"
        val fileBody = RequestBody.create(MultipartFormData, file)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", URLEncoder.encode(file.name, "utf-8"), fileBody)
            .build()

        val request = buildPost(path = url, requestBody = requestBody)
        val message = MessageUtil.getMessageByLocale(UPLOAD_CUSTOM_FILE_FAILED, AgentEnv.getLocaleLanguage())
        val response = request(request, message)
        try {
            val obj = JsonParser.parseString(response).asJsonObject
            if (obj.has("code") && obj["code"].asString != "200") {
                throw RemoteServiceException(message)
            }
        } catch (ignored: Exception) {
            LoggerService.addNormalLine(ignored.message ?: "")
            throw RemoteServiceException("archive fail: $response")
        }
    }

    override fun uploadPipeline(file: File, buildVariables: BuildVariables, token: String?) {
        LoggerService.addNormalLine("upload file >>> ${file.name}")
        val url = "/ms/artifactory/api/build/artifactories/file/archive?fileType=${FileTypeEnum.BK_ARCHIVE}"
        val fileBody = RequestBody.create(MultipartFormData, file)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", URLEncoder.encode(file.name, "utf-8"), fileBody)
            .build()

        val request = buildPost(path = url, requestBody = requestBody)
        val message = MessageUtil.getMessageByLocale(
            UPLOAD_PIPELINE_FILE_FAILED,
            AgentEnv.getLocaleLanguage()
        )
        val response = request(request, message)
        try {
            val obj = JsonParser.parseString(response).asJsonObject
            if (obj.has("code") && obj["code"].asString != "200") throw RemoteServiceException(message)
        } catch (ignored: Exception) {
            LoggerService.addNormalLine(ignored.message ?: "")
        }
    }

    override fun uploadLog(file: File, destFullPath: String, buildVariables: BuildVariables, token: String?) {
        logger.warn("uploadLog not implemented")
    }

    override fun downloadCustomizeFile(
        userId: String,
        projectId: String,
        uri: String,
        destPath: File,
        isVmBuildEnv: Boolean,
        token: String?
    ) {
        val url = if (uri.startsWith("http://") || uri.startsWith("https://")) {
            uri
        } else {
            "/ms/artifactory/api/build/artifactories/file/archive/download" +
                    "?fileType=${FileTypeEnum.BK_CUSTOM}&customFilePath=$uri"
        }
        val request = buildGet(url)
        download(request, destPath)
    }

    override fun downloadPipelineFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        uri: String,
        destPath: File,
        isVmBuildEnv: Boolean,
        token: String?
    ) {
        val url = if (uri.startsWith("http://") || uri.startsWith("https://")) {
            uri
        } else {
            "/ms/artifactory/api/build/artifactories/file/archive/download" +
                    "?fileType=${FileTypeEnum.BK_ARCHIVE}&customFilePath=$uri"
        }
        val request = buildGet(url)
        download(request, destPath)
    }

    override fun dockerBuildCredential(projectId: String): Map<String, String> {
        return hashMapOf()
    }

    override fun uploadFile(
        url: String,
        file: File,
        headers: Map<String, String>?,
        isVmBuildEnv: Boolean?
    ): Result<Boolean> {
        LoggerService.addNormalLine("upload file url >>> $url")
        val fileBody = RequestBody.create(MultipartFormData, file)
        val fileName = file.name
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBody)
            .build()
        val request = buildPost(
            path = url,
            requestBody = requestBody,
            headers = headers ?: emptyMap(),
            useFileDevnetGateway = isVmBuildEnv
        )
        val responseContent = request(request, "upload file:$fileName fail")
        return objectMapper.readValue(responseContent)
    }
}

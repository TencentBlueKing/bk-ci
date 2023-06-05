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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_CREDENTIAL_INFO_FAILED
import com.tencent.devops.worker.common.constants.WorkerMessageCode.UPLOAD_CUSTOM_FILE_FAILED
import com.tencent.devops.worker.common.constants.WorkerMessageCode.UPLOAD_PIPELINE_FILE_FAILED
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@ApiPriority(priority = 9)
class BkRepoArchiveResourceApi : AbstractBuildResourceApi(), ArchiveSDKApi {
    private val bkrepoResourceApi = BkRepoResourceApi()

    private fun getParentFolder(path: String): String {
        val tmpPath = path.removeSuffix("/")
        return tmpPath.removeSuffix(getFileName(tmpPath))
    }

    private fun getFileName(path: String): String {
        return path.removeSuffix("/").split("/").last()
    }

    override fun getRealm(): String {
        return REALM_BK_REPO
    }

    override fun getFileDownloadUrls(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?
    ): List<String> {
        val repoName: String
        val filePath: String
        val fileName: String
        if (fileType == FileTypeEnum.BK_CUSTOM) {
            repoName = "custom"
            val normalizedPath = "/${customFilePath!!.removePrefix("./").removePrefix("/")}"
            filePath = getParentFolder(normalizedPath)
            fileName = getFileName(normalizedPath)
        } else {
            repoName = "pipeline"
            filePath = "/$pipelineId/$buildId/"
            fileName = getFileName(customFilePath!!)
        }

        return bkrepoResourceApi.queryByPathEqOrNameMatchOrMetadataEqAnd(
            userId = userId,
            projectId = projectId,
            repoNames = listOf(repoName),
            filePaths = listOf(filePath),
            fileNames = listOf(fileName),
            metadata = mapOf(),
            page = 0,
            pageSize = 10000
        ).map { it.fullPath }
    }

    private fun uploadBkRepoCustomize(file: File, destPath: String, buildVariables: BuildVariables) {
        val bkRepoPath = destPath.removeSuffix("/") + "/" + file.name
        val url = "/bkrepo/api/build/generic/${buildVariables.projectId}/custom/$bkRepoPath"
        val request = buildPut(
            url,
            file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
            bkrepoResourceApi.getUploadHeader(file, buildVariables, true)
        )
        val message = MessageUtil.getMessageByLocale(
            UPLOAD_CUSTOM_FILE_FAILED,
            AgentEnv.getLocaleLanguage()
        )
        val response = request(request, message)
        try {
            val obj = objectMapper.readTree(response)
            if (obj.has("code") && obj["code"].asText() != "0") throw RemoteServiceException(message)
        } catch (e: Exception) {
            logger.error(e.message ?: "")
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "archive fail: $response"
            )
        }
    }

    override fun uploadCustomize(file: File, destPath: String, buildVariables: BuildVariables, token: String?) {
        if (!token.isNullOrBlank()) {
            val relativePath = destPath.removePrefix("/").removePrefix("./").removeSuffix("/")
            val destFullPath = "/$relativePath/${file.name}"
            bkrepoResourceApi.uploadFileByToken(
                file = file,
                projectId = buildVariables.projectId,
                repoName = "custom",
                destFullPath = destFullPath,
                token = token,
                buildVariables = buildVariables,
                parseAppMetadata = true
            )
        } else {
            uploadBkRepoCustomize(file, destPath, buildVariables)
        }
    }

    override fun uploadPipeline(file: File, buildVariables: BuildVariables, token: String?) {
        if (!token.isNullOrBlank()) {
            val destFullPath = "/${buildVariables.pipelineId}/${buildVariables.buildId}/${file.name}"
            bkrepoResourceApi.uploadFileByToken(
                file = file,
                projectId = buildVariables.projectId,
                repoName = "pipeline",
                destFullPath = destFullPath,
                token = token,
                buildVariables = buildVariables,
                parseAppMetadata = true
            )
        } else {
            uploadBkRepoPipeline(file, buildVariables)
        }
        bkrepoResourceApi.setPipelineMetadata("pipeline", buildVariables)
    }

    private fun uploadBkRepoPipeline(file: File, buildVariables: BuildVariables) {
        logger.info("upload file >>> ${file.name}")
        val url = "/bkrepo/api/build/generic/${buildVariables.projectId}/pipeline/${buildVariables.pipelineId}/" +
            "${buildVariables.buildId}/${file.name}"
        val request = buildPut(
            url,
            file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
            bkrepoResourceApi.getUploadHeader(file, buildVariables, true)
        )
        val message = MessageUtil.getMessageByLocale(
            UPLOAD_PIPELINE_FILE_FAILED,
            AgentEnv.getLocaleLanguage()
        )
        val response = request(request, message)
        try {
            val obj = objectMapper.readTree(response)
            if (obj.has("code") && obj["code"].asText() != "0") throw RemoteServiceException(message)
        } catch (e: Exception) {
            logger.error(e.message ?: "")
        }
    }

    override fun uploadLog(file: File, destFullPath: String, buildVariables: BuildVariables, token: String?) {
        bkrepoResourceApi.uploadBkRepoFile(
            file = file,
            repoName = "log",
            destFullPath = destFullPath,
            buildVariables = buildVariables,
            parseAppMetadata = false,
            token = token
        )
    }

    private fun downloadBkRepoFile(
        user: String,
        projectId: String,
        repoName: String,
        fullpath: String,
        destPath: File,
        isVmBuildEnv: Boolean
    ) {
        val url = "/bkrepo/api/build/generic/$projectId/$repoName$fullpath"
        val header = HashMap<String, String>()
        header["X-BKREPO-UID"] = user
        val request = buildGet(url, header, isVmBuildEnv)
        download(request, destPath)
    }

    override fun downloadCustomizeFile(
        userId: String,
        projectId: String,
        uri: String,
        destPath: File,
        isVmBuildEnv: Boolean,
        token: String?
    ) {
        if (!token.isNullOrBlank()) {
            bkrepoResourceApi.downloadFileByToken(
                userId = userId,
                projectId = projectId,
                repoName = "custom",
                fullPath = uri,
                token = token,
                destPath = destPath,
                isVmBuildEnv = isVmBuildEnv
            )
        } else {
            downloadBkRepoFile(
                user = userId,
                projectId = projectId,
                repoName = "custom",
                fullpath = uri,
                destPath = destPath,
                isVmBuildEnv = isVmBuildEnv
            )
        }
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
        if (!token.isNullOrBlank()) {
            bkrepoResourceApi.downloadFileByToken(
                userId = userId,
                projectId = projectId,
                repoName = "pipeline",
                fullPath = uri,
                token = token,
                destPath = destPath,
                isVmBuildEnv = isVmBuildEnv
            )
        } else {
            downloadBkRepoFile(
                user = userId,
                projectId = projectId,
                repoName = "pipeline",
                fullpath = uri,
                destPath = destPath,
                isVmBuildEnv = isVmBuildEnv
            )
        }
    }

    /*
     * 此处绑定了jfrog的plugin实现接口，用于给用户颁发临时密钥用于docker push
     */
    override fun dockerBuildCredential(projectId: String): Map<String, String> {
        val path = "/dockerbuild/credential"
        val request = buildGet(path)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(GET_CREDENTIAL_INFO_FAILED, AgentEnv.getLocaleLanguage())
        )
        return jacksonObjectMapper().readValue(responseContent)
    }

    override fun uploadFile(
        url: String,
        file: File,
        headers: Map<String, String>?,
        isVmBuildEnv: Boolean?
    ): Result<Boolean> {
        LoggerService.addNormalLine("upload file url >>> $url")
        val fileBody = file.asRequestBody(MultipartFormData)
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
        val responseContent = request(request, "upload file[$fileName] failed")
        return objectMapper.readValue(responseContent)
    }
}

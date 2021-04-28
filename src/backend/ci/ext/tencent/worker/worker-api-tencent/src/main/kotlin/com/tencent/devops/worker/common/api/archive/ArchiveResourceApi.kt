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
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

@ApiPriority(priority = 9)
class ArchiveResourceApi : AbstractBuildResourceApi(), ArchiveSDKApi {
    private val bkRepoResourceApi = BkRepoResourceApi()

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

        return bkRepoResourceApi.queryByPathEqOrNameMatchOrMetadataEqAnd(
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

    override fun uploadCustomize(file: File, destPath: String, buildVariables: BuildVariables) {
        val relativePath = destPath.removePrefix("/").removePrefix("./").removeSuffix("/")
        val destFullPath = "/$relativePath/${file.name}"
        bkRepoResourceApi.uploadBkRepoFile(
            file = file,
            repoName = "custom",
            destFullPath = destFullPath,
            tokenAuthPath = "/",
            buildVariables = buildVariables,
            parseAppMetadata = true
        )
    }

    override fun uploadPipeline(file: File, buildVariables: BuildVariables) {
        bkRepoResourceApi.uploadBkRepoFile(
            file = file,
            repoName = "pipeline",
            destFullPath = "/${buildVariables.pipelineId}/${buildVariables.buildId}/${file.name}",
            tokenAuthPath = "/${buildVariables.pipelineId}/${buildVariables.buildId}",
            buildVariables = buildVariables,
            parseAppMetadata = true
        )
        bkRepoResourceApi.setPipelineMetadata("pipeline", buildVariables)
    }

    override fun uploadLog(file: File, destFullPath: String, buildVariables: BuildVariables) {
        bkRepoResourceApi.uploadBkRepoFile(
            file = file,
            repoName = "log",
            destFullPath = destFullPath,
            tokenAuthPath = "/",
            buildVariables = buildVariables,
            parseAppMetadata = false
        )
    }

    override fun downloadCustomizeFile(
        userId: String,
        projectId: String,
        uri: String,
        destPath: File
    ) {
        bkRepoResourceApi.downloadBkRepoFile(userId, projectId, "custom", uri, destPath)
    }

    override fun downloadPipelineFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        uri: String,
        destPath: File
    ) {
        bkRepoResourceApi.downloadBkRepoFile(userId, projectId, "pipeline", uri, destPath)
    }

    /*
     * 此处绑定了jfrog的plugin实现接口，用于给用户颁发临时密钥用于docker push
     */
    override fun dockerBuildCredential(projectId: String): Map<String, String> {
        val path = "/dockerbuild/credential"
        val request = buildGet(path)
        val responseContent = request(request, "获取凭证信息失败")
        return jacksonObjectMapper().readValue(responseContent)
    }

    override fun uploadFile(
        url: String,
        destPath: String,
        file: File,
        headers: Map<String, String>?
    ): Result<Boolean> {
        LoggerService.addNormalLine("upload file url >>> $url")
        val fileBody = RequestBody.create(MultipartFormData, file)
        val fileName = file.name
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBody)
            .build()
        val request = buildPost(url, requestBody, headers ?: emptyMap(), useFileGateway = true)
        val responseContent = request(request, "upload file[$fileName] failed")
        return objectMapper.readValue(responseContent)
    }

    private fun getParentFolder(path: String): String {
        val tmpPath = path.removeSuffix("/")
        return tmpPath.removeSuffix(getFileName(tmpPath))
    }

    private fun getFileName(path: String): String {
        return path.removeSuffix("/").split("/").last()
    }
}

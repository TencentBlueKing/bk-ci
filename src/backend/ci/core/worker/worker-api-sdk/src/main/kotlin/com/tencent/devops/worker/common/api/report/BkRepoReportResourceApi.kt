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

package com.tencent.devops.worker.common.api.report

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.report.ReportEmail
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.api.archive.BkRepoResourceApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.CREATE_REPORT_FAIL
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_REPORT_ROOT_PATH_FAILURE
import com.tencent.devops.worker.common.constants.WorkerMessageCode.UPLOAD_CUSTOM_REPORT_FAILURE
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.logger.LoggerService.elementId
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.nio.file.Files

@ApiPriority(priority = 9)
class BkRepoReportResourceApi : AbstractBuildResourceApi(), ReportSDKApi {
    private val bkrepoResourceApi = BkRepoResourceApi()

    override fun getRealm(): String {
        return REALM_BK_REPO
    }

    override fun getRootUrl(taskId: String): Result<String> {
        val path = "/ms/process/api/build/reports/$taskId/rootUrl"
        val request = buildGet(path)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(GET_REPORT_ROOT_PATH_FAILURE, AgentEnv.getLocaleLanguage())
        )
        return objectMapper.readValue(responseContent)
    }

    override fun createReportRecord(
        buildVariables: BuildVariables,
        taskId: String,
        indexFile: String,
        name: String,
        reportType: String?,
        reportEmail: ReportEmail?,
        token: String?
    ): Result<Boolean> {
        createReportRecordToBkRepo(
            buildVariables = buildVariables,
            taskId = taskId,
            indexFile = indexFile,
            reportName = name,
            reportType = reportType,
            token = token
        )
        val indexFileEncode = encode(indexFile)
        val nameEncode = encode(name)
        val path =
            "/ms/process/api/build/reports/$taskId?indexFile=$indexFileEncode&name=$nameEncode&reportType=$reportType"
        val request = if (reportEmail == null) {
            buildPost(path)
        } else {
            val requestBody = objectMapper.writeValueAsString(reportEmail)
                .toRequestBody(JsonMediaType)
            buildPost(path, requestBody)
        }
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(CREATE_REPORT_FAIL, AgentEnv.getLocaleLanguage())
        )
        return objectMapper.readValue(responseContent)
    }

    private fun createReportRecordToBkRepo(
        buildVariables: BuildVariables,
        taskId: String,
        indexFile: String,
        reportName: String,
        reportType: String?,
        token: String?
    ) {
        val userId = buildVariables.variables[PIPELINE_START_USER_ID].toString()
        val projectId = buildVariables.projectId
        val pipelineId = buildVariables.pipelineId
        val buildId = buildVariables.buildId
        val metadata = mutableMapOf<String, String>()
        metadata.putAll(bkrepoResourceApi.getPipelineMetadata(buildVariables, taskId))
        metadata["reportName"] = reportName
        metadata["reportType"] = reportType ?: ReportTypeEnum.INTERNAL.name

        val fullPath = if (reportType == ReportTypeEnum.THIRDPARTY.name) {
            metadata["reportUrl"] = indexFile
            val emptyFile = Files.createTempFile("report", ".tmp").toFile()
            val filePath = "/$pipelineId/$buildId/$taskId/index.html"
            bkrepoResourceApi.uploadBkRepoFile(emptyFile, "report", filePath, buildVariables, false, token)
            emptyFile.delete()
            filePath
        } else {
            "/$pipelineId/$buildId/$taskId/$indexFile"
        }

        bkrepoResourceApi.saveMetadata(userId, projectId, "report", fullPath, metadata)
    }

    private fun uploadBkRepoReportByToken(
        file: File,
        token: String,
        relativePath: String,
        buildVariables: BuildVariables
    ) {
        val pipelineId = buildVariables.pipelineId
        val buildId = buildVariables.buildId
        bkrepoResourceApi.uploadFileByToken(
            file = file,
            projectId = buildVariables.projectId,
            repoName = "report",
            destFullPath = "/$pipelineId/$buildId/$elementId/${relativePath.removePrefix("/")}",
            token = token,
            buildVariables = buildVariables,
            parseAppMetadata = false,
            parsePipelineMetadata = false
        )
    }

    private fun uploadBkRepoReport(file: File, relativePath: String, buildVariables: BuildVariables) {
        val projectId = buildVariables.projectId
        val pipelineId = buildVariables.pipelineId
        val buildId = buildVariables.buildId
        val path = relativePath.removePrefix("/")
        val url = "/bkrepo/api/build/generic/$projectId/report/$pipelineId/$buildId/$elementId/$path"

        val request = buildPut(
            path = url,
            requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
            headers = bkrepoResourceApi.getUploadHeader(
                file = file,
                buildVariables = buildVariables,
                parseAppMetadata = false,
                parsePipelineMetadata = false
            )
        )
        val message = MessageUtil.getMessageByLocale(UPLOAD_CUSTOM_REPORT_FAILURE, AgentEnv.getLocaleLanguage())
        val responseContent = request(request, message)
        try {
            val obj = objectMapper.readTree(responseContent)
            if (obj.has("code") && obj["code"].asText() != "0") throw RemoteServiceException(message)
        } catch (e: Exception) {
            LoggerService.addNormalLine(e.message ?: "")
            throw RemoteServiceException("report archive fail: $responseContent")
        }
    }

    override fun uploadReport(
        file: File,
        taskId: String,
        relativePath: String,
        buildVariables: BuildVariables,
        token: String?
    ) {
        if (bkrepoResourceApi.tokenAccess()) {
            uploadBkRepoReportByToken(file, token!!, relativePath, buildVariables)
        } else {
            uploadBkRepoReport(file, relativePath, buildVariables)
        }
        bkrepoResourceApi.setPipelineMetadata("report", buildVariables)
    }
}

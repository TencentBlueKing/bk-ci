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
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.report.ReportEmail
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.api.archive.BkRepoResourceApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.logger.LoggerService.elementId
import com.tencent.devops.worker.common.utils.TaskUtil
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import java.io.File

@ApiPriority(priority = 9)
class BkRepoReportResourceApi : AbstractBuildResourceApi(), ReportSDKApi {
    private val bkrepoResourceApi = BkRepoResourceApi()

    override fun getRealm(): String {
        return REALM_BK_REPO
    }

    override fun getRootUrl(taskId: String): Result<String> {
        val path = "/ms/process/api/build/reports/$taskId/rootUrl"
        val request = buildGet(path)
        val responseContent = request(request, "获取报告跟路径失败")
        return objectMapper.readValue(responseContent)
    }

    override fun createReportRecord(
        taskId: String,
        indexFile: String,
        name: String,
        reportType: String?,
        reportEmail: ReportEmail?
    ): Result<Boolean> {
        val indexFileEncode = encode(indexFile)
        val nameEncode = encode(name)
        val path =
            "/ms/process/api/build/reports/$taskId?indexFile=$indexFileEncode&name=$nameEncode&reportType=$reportType"
        val request = if (reportEmail == null) {
            buildPost(path)
        } else {
            val requestBody = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                objectMapper.writeValueAsString(reportEmail)
            )
            buildPost(path, requestBody)
        }
        val responseContent = request(request, "创建报告失败")
        return objectMapper.readValue(responseContent)
    }

    private fun uploadBkRepoReportByToken(
        file: File,
        token: String,
        task: String,
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
            parseAppMetadata = false
        )
    }

    private fun uploadBkRepoReport(file: File, taskId: String, relativePath: String, buildVariables: BuildVariables) {
        val projectId = buildVariables.projectId
        val pipelineId = buildVariables.pipelineId
        val buildId = buildVariables.buildId
        val path = relativePath.removePrefix("/")
        val url = "/bkrepo/api/build/generic/$projectId/report/$pipelineId/$buildId/$elementId/$path"

        val request = buildPut(
            path = url,
            requestBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file),
            headers = bkrepoResourceApi.getUploadHeader(file, buildVariables, parseAppMetadata = false),
            useFileDevnetGateway = TaskUtil.isVmBuildEnv(buildVariables.containerType)
        )
        val responseContent = request(request, "上传自定义报告失败")
        try {
            val obj = objectMapper.readTree(responseContent)
            if (obj.has("code") && obj["code"].asText() != "0") throw RemoteServiceException("上传自定义报告失败")
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
            uploadBkRepoReportByToken(file, token!!, taskId, relativePath, buildVariables)
        } else {
            uploadBkRepoReport(file, taskId, relativePath, buildVariables)
        }
        bkrepoResourceApi.setPipelineMetadata("report", buildVariables)
    }
}

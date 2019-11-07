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

package com.tencent.devops.worker.common.api.report

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_NO
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PROJECT_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_SOURCE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_USER_ID
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.report.ReportEmail
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File

@ApiPriority(priority = 9)
class TencentReportResourceApi : AbstractBuildResourceApi(), ReportSDKApi {

    override fun getRootUrl(taskId: String): Result<String> {
        val path = "/ms/artifactory/api/build/artifactories/report/$taskId/root"
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
        val request = buildPost(path)
        val responseContent = request(request, "创建报告失败")
        return objectMapper.readValue(responseContent)
    }

    override fun uploadReport(file: File, taskId: String, relativePath: String, buildVariables: BuildVariables) {
        val url = StringBuilder("/report/result/$taskId/${relativePath.removePrefix("/")}")
        with(buildVariables) {
            url.append(";$ARCHIVE_PROPS_PROJECT_ID=${encodeProperty(projectId)}")
            url.append(";$ARCHIVE_PROPS_PIPELINE_ID=${encodeProperty(pipelineId)}")
            url.append(";$ARCHIVE_PROPS_BUILD_ID=${encodeProperty(buildId)}")
            url.append(";$ARCHIVE_PROPS_USER_ID=${encodeProperty(variables[PIPELINE_START_USER_ID] ?: "")}")
            url.append(";$ARCHIVE_PROPS_BUILD_NO=${encodeProperty(variables[PIPELINE_BUILD_NUM] ?: "")}")
            url.append(";$ARCHIVE_PROPS_SOURCE=pipeline")
        }

        val request = buildPut(url.toString(), RequestBody.create(MediaType.parse("application/octet-stream"), file))
        val responseContent = request(request, "上传自定义报告失败")
        try {
            val obj = JsonParser().parse(responseContent).asJsonObject
            if (obj.has("code") && obj["code"].asString != "200") throw RuntimeException()
        } catch (e: Exception) {
            LoggerService.addNormalLine(e.message ?: "")
            throw RuntimeException("report archive fail: $responseContent")
        }
    }
}
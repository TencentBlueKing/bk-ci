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
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class ReportResourceApi : AbstractBuildResourceApi(), ReportSDKApi {

    override fun uploadReport(file: File, taskId: String, relativePath: String, buildVariables: BuildVariables) {
        val purePath = "$taskId/${purePath(relativePath)}"
        logger.info("[${buildVariables.buildId}]| purePath=$purePath")
        val url =
            "/ms/artifactory/api/build/artifactories/file/archive?fileType=${FileTypeEnum.BK_REPORT}&customFilePath=$purePath"

        val fileBody = RequestBody.create(MultipartFormData, file)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, fileBody)
            .build()

        val request = buildPost(url, requestBody)

        val response = request(request, "上传自定义报告失败")

        try {
            val obj = JsonParser().parse(response).asJsonObject
            if (obj.has("code") && obj["code"].asString != "200") throw RemoteServiceException("上传流水线文件失败")
        } catch (ignored: Exception) {
            LoggerService.addNormalLine(ignored.message ?: "")
            throw RemoteServiceException("report archive fail: $response")
        }
    }

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
        reportType: String?
    ): Result<Boolean> {
        val indexFileEncode = encode(indexFile)
        val nameEncode = encode(name)
        val path =
            "/ms/process/api/build/reports/$taskId?indexFile=$indexFileEncode&name=$nameEncode&reportType=$reportType"
        val request = buildPost(path)
        val responseContent = request(request, "创建报告失败")
        return objectMapper.readValue(responseContent)
    }
}
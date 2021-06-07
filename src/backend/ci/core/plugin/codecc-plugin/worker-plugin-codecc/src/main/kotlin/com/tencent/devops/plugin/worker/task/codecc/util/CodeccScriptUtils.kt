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

package com.tencent.devops.plugin.worker.task.codecc.util

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.codecc.CodeccSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File

@Suppress("ALL")
class CodeccScriptUtils : AbstractBuildResourceApi() {

    private val api = ApiFactory.create(CodeccSDKApi::class)

    fun downloadScriptFile(codeccWorkspace: File): File {
        val codeccScriptConfig = api.getSingleCodeccScript().data
            ?: throw RuntimeException("get codecc script config error")
        val fileName = codeccScriptConfig.scriptFileName
        val fileSizeUrl = codeccScriptConfig.fileSizeUrl
        val downloadUrl = codeccScriptConfig.downloadUrl
        val codeccHost = codeccScriptConfig.devnetHost

        // 1) get file size
        val fileSizeParams = mapOf(
            "fileName" to fileName,
            "downloadType" to "BUILD_SCRIPT"
        )
        val fileSizeRequest = buildPost(fileSizeUrl, RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            JsonUtil.getObjectMapper().writeValueAsString(fileSizeParams)), mutableMapOf())
            .newBuilder()
            .url("$codeccHost$fileSizeUrl")
            .build()
        val fileSize = OkhttpUtils.doHttp(fileSizeRequest).use {
            val data = it.body()!!.string()
            LoggerService.addNormalLine("get file size data: $data")
            val jsonData = JsonUtil.getObjectMapper().readValue<Map<String, Any>>(data)
            if (jsonData["status"] != 0) {
                throw RuntimeException("get file size fail!")
            }
            jsonData["data"] as Int
        }

        // 2) download
        val downloadParams = mapOf(
            "fileName" to fileName,
            "downloadType" to "BUILD_SCRIPT",
            "beginIndex" to "0",
            "btyeSize" to fileSize
        )
        val downloadRequest = buildPost(path = downloadUrl,
            requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                JsonUtil.getObjectMapper().writeValueAsString(downloadParams)),
            headers = mutableMapOf())
            .newBuilder()
            .url("$codeccHost$downloadUrl")
            .build()
        OkhttpUtils.doHttp(downloadRequest).use {
            val data = it.body()!!.string()
            LoggerService.addNormalLine("get file content success")
            val file = File(codeccWorkspace, fileName)
            file.writeText(data)
            return file
        }
    }
}

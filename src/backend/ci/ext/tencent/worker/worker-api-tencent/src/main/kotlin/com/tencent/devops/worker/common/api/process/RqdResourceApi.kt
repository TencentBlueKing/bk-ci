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

package com.tencent.devops.worker.common.api.process
import com.google.gson.JsonParser
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLEncoder

class RqdResourceApi : AbstractBuildResourceApi() {
    private val logger = LoggerFactory.getLogger(RqdResourceApi::class.java)
    private val parser = JsonParser()

    fun upload(
        file: File,
        appId: String,
        appKey: String,
        fileName: String,
        symbolType: String,
        pid: String,
        version: String,
        bundleId: String,
        elementId: String
    ): Result<String> {
        val path = "rqd/upload?app_key=$appKey&app_id=$appId"
//        val headers = mapOf(
//                "appId" to appId,
//                "appKey" to appKey,
//                "fileName" to fileName,
//                "symbolType" to symbolType,
//                "version" to version,not Info.plist found
//                "bundleId" to bundleId,
//                "elementId" to elementId
//        )
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("api_version", "1")
            .addFormDataPart("productVersion", URLEncoder.encode(version, "UTF-8"))
            .addFormDataPart("bundleId", URLEncoder.encode(bundleId, "UTF-8"))
            .addFormDataPart("app_id", appId)
            .addFormDataPart("app_key", appKey)
            .addFormDataPart("symbolType", symbolType)
            .addFormDataPart("pid", pid)
            .addFormDataPart("mappingName", fileName)
            .addFormDataPart("mappingMd5", FileUtil.getMD5(file))
            .addFormDataPart("file", file.name, RequestBody.create(MediaType.parse("application/octet-stream"), file))
            .build()

        val request = buildPost(path, body)
        val responseContent = request(request, "上传rqd文件失败")

        val obj = parser.parse(responseContent).asJsonObject
        return if (obj["rtcode"].asString != "0") {
            Result(status = obj["rtcode"].asInt, message = null, data = responseContent)
        } else {
            Result(responseContent)
        }
    }
}
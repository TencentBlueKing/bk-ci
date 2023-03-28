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

package com.tencent.devops.worker.common.api.bugly

import com.google.gson.JsonParser
import com.tencent.devops.common.api.constant.I18NConstant.BK_FAILED_UPLOAD_BUGLY_FILE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLEncoder

class BuglyResourceApi : AbstractBuildResourceApi() {
    private val logger = LoggerFactory.getLogger(BuglyResourceApi::class.java)
    private val parser = JsonParser()

    fun upload(
        file: File,
        appId: String,
        appKey: String,
        fileName: String,
        symbolType: String,
        version: String,
        bundleId: String,
        elementId: String
    ): Result<String> {
        val path = "bugly/upload?app_key=$appKey&app_id=$appId"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("api_version", "1")
            .addFormDataPart("request_send_timestamp", System.currentTimeMillis().toString().substring(0, 10))
            .addFormDataPart("productVersion", URLEncoder.encode(version, "UTF-8"))
            .addFormDataPart("bundleId", URLEncoder.encode(bundleId, "UTF-8"))
            .addFormDataPart("app_id", appId)
            .addFormDataPart("app_key", appKey)
            .addFormDataPart("symbolType", symbolType)
            .addFormDataPart("fileName", URLEncoder.encode(fileName, "UTF-8"))
            .addFormDataPart("file", file.name, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()
        val request = buildPost(path, body)
        val responseContent = request(request,
            MessageUtil.getMessageByLocale(
                messageCode = BK_FAILED_UPLOAD_BUGLY_FILE,
                language = I18nUtil.getDefaultLocaleLanguage()
            ))

        val obj = parser.parse(responseContent).asJsonObject
        return if (obj["rtcode"].asString != "0") {
            Result(status = obj["rtcode"].asInt, message = null, data = responseContent)
        } else {
            Result(responseContent)
        }
    }
}

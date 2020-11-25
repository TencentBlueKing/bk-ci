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

package com.tencent.devops.plugin.service

import com.tencent.devops.common.client.Client
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Service
class EnterpriseService @Autowired constructor(private val client: Client) {

    companion object {
        private val logger = LoggerFactory.getLogger(EnterpriseService::class.java)
    }

    @Value("\${enterprise.url}")
    private val ipList = ""

    @Value("\${enterprise.env}")
    private val env = ""

    fun upload(
        fileStream: InputStream,
        md5: String,
        size: Long,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        fileName: String,
        props: String
    ): String {

        // 调用接口上传ipa包
        ipList.split(",").map { it.trim() }.shuffled().forEach { ip ->
            val url = "http://$ip/upload?projectId=$projectId&pipelineId=$pipelineId&" +
                    "buildId=$buildId&size=$size&md5=$md5&env=$env&properties=$props"

            logger.info("request url >>> $url")

            val fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), fileStream.readBytes())
            val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, fileBody)
                    .build()
            val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

            val timeout = (1 + size / 1024 / 1024 / 1024) * 7
            val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(100, TimeUnit.SECONDS)
                    .writeTimeout(timeout, TimeUnit.MINUTES)
                    .readTimeout(timeout, TimeUnit.MINUTES)
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                val data = response.body()!!.string()
                logger.info("data>>>> $data")
                if (response.isSuccessful && data == "success") {
                    return "success"
                }
            }
        }
        throw RuntimeException("enterprise sign fail")
    }
}

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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.remotedev.pojo.windows.WindowsDevCouldCallback
import java.time.LocalDateTime
import java.util.concurrent.Executors
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class HttpCallBackService @Autowired constructor() {
    @Value("\${remoteDev.devCloudCallback.url:}")
    private val url: String = ""

    @Value("\${remoteDev.devCloudCallback.appId:}")
    private val appId: String = ""

    @Value("\${remoteDev.devCloudCallback.token:}")
    private val token: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(HttpCallBackService::class.java)
    }

    private val executor = Executors.newCachedThreadPool()

    fun asyncTask(callback: WindowsDevCouldCallback) {
        executor.submit {
            kotlin.runCatching { callOtherPlatformCallback(callback) }.onFailure {
                logger.warn("HttpCallBackService fail| ${it.message}", it)
            }
        }
    }

    private fun callOtherPlatformCallback(callback: WindowsDevCouldCallback) {
        if (url.isBlank()) return
        val jsonString = JsonUtil.toJson(callback)
        val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
        val sendRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .headers(devCloudHeader())
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("call back fail.|$url|$jsonString|$responseContent")
            }
        }
    }

    private fun devCloudHeader(): Headers {
        val timestamp = LocalDateTime.now().timestamp().toString()
        val randomString = RandomStringUtils.randomAlphanumeric(8)
        val userId = "landun"
        val encKey = DigestUtils.md5Hex("$token$timestamp$randomString")
        return mapOf(
            "APPID" to appId,
            "USERID" to userId,
            "RANDOM" to randomString,
            "TIMESTP" to timestamp,
            "ENCKEY" to encKey
        ).toHeaders()
    }
}

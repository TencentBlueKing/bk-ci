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

package com.tencent.devops.common.environment.agent.client

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory

object DevCloudContainerInstanceClient {

    private val logger = LoggerFactory.getLogger(DevCloudContainerInstanceClient::class.java)

    fun getContainerInstance(
        devCloudUrl: String,
        devCloudAppId: String,
        devCloudToken: String,
        staffName: String,
        id: String,
        smartProxyToken: String
    ): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers/$id/instances"
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
//            .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
            .headers(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken).toHeaders())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw TaskExecuteException(
                    errorCode = ErrorCode.THIRD_PARTY_INTERFACE_ERROR,
                    errorType = ErrorType.THIRD_PARTY,
                    errorMsg = "Fail to get container status"
                )
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun getHeaders(appId: String, token: String, staffName: String): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["APPID"] = appId
        val random = RandomStringUtils.randomAlphabetic(8)
        headerBuilder["RANDOM"] = random
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        headerBuilder["TIMESTP"] = timestamp
        headerBuilder["STAFFNAME"] = staffName
        val encKey = DigestUtils.md5Hex("$token$timestamp$random")
        headerBuilder["ENCKEY"] = encKey

        return headerBuilder
    }
}

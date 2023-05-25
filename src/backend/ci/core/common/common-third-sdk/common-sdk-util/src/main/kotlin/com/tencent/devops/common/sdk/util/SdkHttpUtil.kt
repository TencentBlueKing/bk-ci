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

package com.tencent.devops.common.sdk.util

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.sdk.SdkRequest
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.enums.HttpStatus
import com.tencent.devops.common.sdk.exception.SdkException
import com.tencent.devops.common.sdk.exception.SdkNotFoundException
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.lang.reflect.ParameterizedType
import java.util.concurrent.TimeUnit

object SdkHttpUtil {
    private const val connectTimeout = 5L
    private const val readTimeout = 30L
    private const val writeTimeout = 30L
    private val logger = LoggerFactory.getLogger(SdkHttpUtil::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .build()

    fun buildGet(url: String, headers: Map<String, String>? = null): Request {
        return build(url, headers).get().build()
    }

    fun buildGet(url: String, headers: Map<String, String>? = null, params: Map<String, Any>? = null): Request {
        val targetUrl = if (!params.isNullOrEmpty()) {
            val urlParams = params.map { entry -> "${entry.key}=${entry.value}" }.joinToString("&")
            if (url.contains("?")) {
                "$url&$urlParams"
            } else {
                "$url?$urlParams"
            }
        } else {
            url
        }
        return build(targetUrl, headers).get().build()
    }

    fun buildPost(url: String, requestBody: RequestBody, headers: Map<String, String>? = null): Request {
        return build(url, headers).post(requestBody).build()
    }

    fun buildPatch(url: String, requestBody: RequestBody, headers: Map<String, String>? = null): Request {
        return build(url, headers).patch(requestBody).build()
    }

    fun buildPut(url: String, requestBody: RequestBody, headers: Map<String, String>? = null): Request {
        return build(url, headers).put(requestBody).build()
    }

    fun buildDelete(url: String, requestBody: RequestBody, headers: Map<String, String>? = null): Request {
        return build(url, headers).delete(requestBody).build()
    }

    fun build(url: String, headers: Map<String, String>? = null): Request.Builder {
        val builder = Request.Builder().url(url)
        if (headers != null) {
            builder.headers(headers.toHeaders())
        }
        return builder
    }

    fun request(request: Request): String {
        val response = okHttpClient.newCall(request).execute()
        return response.use { resp ->
            val responseContent = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                logger.error(
                    "Fail to request(${request.url})" +
                        " with code ${resp.code} message ${resp.message} and response $responseContent"
                )
                if (resp.code == HttpStatus.NOT_FOUND.statusCode) {
                    throw SdkNotFoundException(errMsg = responseContent)
                }
                throw SdkException(errCode = resp.code, errMsg = responseContent)
            }
            responseContent
        }
    }

    /**
     * 生成post请求体对象
     */
    fun generaRequestBody(jsonStr: String, mediaType: String = "application/json"): RequestBody {
        return RequestBody.create(mediaType.toMediaTypeOrNull(), jsonStr)
    }

    /**
     * 执行请求
     *
     * @param apiUrl 请求url
     * @param systemHeaders 系统请求头,所有接口都依赖的公共请求头
     * @param systemParams 系统请求参数,所有接口都依赖的公共请求参数
     * @param request 用户请求参数对象
     */
    fun <T> execute(
        apiUrl: String,
        systemHeaders: Map<String, String> = mapOf(),
        systemParams: Map<String, String> = mapOf(),
        request: SdkRequest<T>
    ): T {
        val headers = mutableMapOf<String, String>()
        headers.putAll(systemHeaders)
        headers.putAll(request.getHeaderMap())

        val params = mutableMapOf<String, Any>()
        val requestParams =
            SdkJsonUtil.fromJson(SdkJsonUtil.toJson(request), object : TypeReference<Map<String, Any>>() {})
        params.putAll(systemParams)
        params.putAll(requestParams)
        params.putAll(request.getUdfParams())

        val finalUrl = "${apiUrl.removeSuffix("/")}/${request.getApiPath().removePrefix("/")}"
        val httpRequest = when (request.getHttpMethod()) {
            HttpMethod.GET ->
                buildGet(url = finalUrl, headers = headers, params = params)
            HttpMethod.POST -> {
                val requestBody = generaRequestBody(jsonStr = SdkJsonUtil.toJson(params))
                buildPost(url = finalUrl, requestBody = requestBody, headers = headers)
            }
            HttpMethod.PUT -> {
                val requestBody = generaRequestBody(jsonStr = SdkJsonUtil.toJson(params))
                buildPut(url = finalUrl, requestBody = requestBody, headers = headers)
            }
            HttpMethod.DELETE -> {
                val requestBody = generaRequestBody(jsonStr = SdkJsonUtil.toJson(params))
                buildDelete(url = finalUrl, requestBody = requestBody, headers = headers)
            }
        }
        val actualType = (request.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
        return SdkJsonUtil.fromJson(request(httpRequest), actualType)
    }
}

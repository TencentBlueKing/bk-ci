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

package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.auth.AUTH_HEADER_CODECC_OPENAPI_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_JWT_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_GATEWAY_TAG
import com.tencent.devops.common.api.auth.AUTH_HEADER_IAM_TOKEN
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.security.jwt.JwtManager
import com.tencent.devops.common.service.BkTag
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

@Service
class AuthHttpClientService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val jwtManager: JwtManager,
    private val bkTag: BkTag
) {
    @Value("\${codecc.openapi.token:#{null}}")
    private val codeccOpenApiToken: String = ""

    fun requestForResponse(
        request: Request,
        connectTimeoutInSec: Long? = null,
        readTimeoutInSec: Long? = null,
        writeTimeoutInSec: Long? = null
    ): Response {
        val builder = buildBuilder(
            connectTimeoutInSec = connectTimeoutInSec,
            readTimeoutInSec = readTimeoutInSec,
            writeTimeoutInSec = writeTimeoutInSec
        )
        val httpClient = builder.build()
        try {
            val response = httpClient.newCall(request).execute()
            logger.info(
                "Request($request) with code ${response.code}"
            )
            return response
        } catch (e: UnknownHostException) { // DNS问题导致请求未到达目标，可重试
            logger.error("UnknownHostException|request($request),error is :$e")
            throw e
        } catch (e: ConnectException) {
            logger.error("ConnectException|request($request),error is :$e")
            throw e
        } catch (e: Exception) {
            if (e is SocketTimeoutException && e.message == "timeout") { // 请求没到达服务器而超时，可重试
                logger.error("SocketTimeoutException(timeout)|request($request),error is :$e")
                throw e
            } else if (e is SocketTimeoutException && e.message == "connect timed out") {
                logger.error("SocketTimeoutException(connect timed out)|request($request),error is :$e")
                throw e
            } else {
                logger.error("Fail to request($request),error is :$e", e)
                throw ClientException("Fail to request($request),error is:${e.message}")
            }
        }
    }

    fun request(
        request: Request,
        errorMessage: String,
        connectTimeoutInSec: Long? = null,
        readTimeoutInSec: Long? = null,
        writeTimeoutInSec: Long? = null
    ): String {

        requestForResponse(
            request = request,
            connectTimeoutInSec = connectTimeoutInSec,
            readTimeoutInSec = readTimeoutInSec,
            writeTimeoutInSec = writeTimeoutInSec
        ).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body?.string()
                logger.warn(
                    "Fail to request($request) with code ${response.code} ," +
                        " message ${response.message} and response ($responseContent)"
                )
                throw RemoteServiceException(errorMessage, response.code, responseContent)
            }
            return response.body!!.string()
        }
    }

    fun getJsonRequest(data: Any): RequestBody {
        return RequestBody.create(JsonMediaType, objectMapper.writeValueAsString(data))
    }

    fun buildPost(
        path: String,
        requestBody: RequestBody,
        gateway: String,
        token: String?,
        system: String? = bkciSystem
    ): Request {
        val url = gateway + path
        val tag = bkTag.getFinalTag()
        logger.info("iam callback url: $url,tag:$tag")
        return Request.Builder().url(url).post(requestBody)
            .headers(buildJwtAndToken(token, system!!).toHeaders())
            .build()
    }

    private fun buildJwtAndToken(
        iamToken: String?,
        system: String
    ): Map<String, String> {
        val tag = bkTag.getFinalTag()
        val headerMap = mutableMapOf<String, String>()
        if (system == codeccSystem && codeccOpenApiToken.isNotEmpty()) {
            // codecc回调请求头
            headerMap[AUTH_HEADER_CODECC_OPENAPI_TOKEN] = codeccOpenApiToken
        } else {
            // bkci回调请求头
            if (jwtManager.isAuthEnable()) {
                val jwtToken = jwtManager.getToken() ?: ""
                headerMap[AUTH_HEADER_DEVOPS_JWT_TOKEN] = jwtToken
            }
            if (!iamToken.isNullOrEmpty()) {
                headerMap[AUTH_HEADER_IAM_TOKEN] = iamToken
            }
            if (tag.isNotEmpty()) {
                // 指定回调集群
                headerMap[AUTH_HEADER_GATEWAY_TAG] = tag
            }
        }
        return headerMap
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS) // Set to 15 minutes
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .hostnameVerifier { _, _ -> true }
        .build()

    companion object {
        val JsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        private const val EMPTY = ""
        private const val CONNECT_TIMEOUT = 5L
        private const val READ_TIMEOUT = 1500L
        private const val WRITE_TIMEOUT = 60L
        private val logger = LoggerFactory.getLogger(AuthHttpClientService::class.java)
        private const val bkciSystem = "ci"
        private const val codeccSystem = "codecc"
    }

    private fun buildBuilder(
        connectTimeoutInSec: Long? = null,
        readTimeoutInSec: Long? = null,
        writeTimeoutInSec: Long? = null
    ): Builder {
        val builder = okHttpClient.newBuilder()
        if (connectTimeoutInSec != null) {
            builder.connectTimeout(connectTimeoutInSec, TimeUnit.SECONDS)
        }
        if (readTimeoutInSec != null) {
            builder.readTimeout(readTimeoutInSec, TimeUnit.SECONDS)
        }
        if (writeTimeoutInSec != null) {
            builder.writeTimeout(writeTimeoutInSec, TimeUnit.SECONDS)
        }
        return builder
    }
}

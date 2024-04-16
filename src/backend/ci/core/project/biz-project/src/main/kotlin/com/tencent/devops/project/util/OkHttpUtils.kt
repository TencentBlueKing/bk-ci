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

package com.tencent.devops.project.util

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.util.HttpRetryUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object OkHttpUtils {
    private const val connectTimeout = 3L
    private const val readTimeout = 3L
    private const val writeTimeout = 3L
    private const val MAX_RETRY_COUNT = 3
    private val JSON = "application/json;charset=utf-8".toMediaTypeOrNull()
    private val logger = LoggerFactory.getLogger(OkHttpClient::class.java)

    private val trustAnyCerts = arrayOf<TrustManager>(object : X509TrustManager {

        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .sslSocketFactory(anySslSocketFactory(), trustAnyCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private fun anySslSocketFactory(): SSLSocketFactory {
        try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAnyCerts, java.security.SecureRandom())
            return sslContext.socketFactory
        } catch (ignored: Exception) {
            throw RemoteServiceException(ignored.message!!)
        }
    }

    private fun buildRequest(
        method: String,
        url: String,
        requestBodyStr: String,
        headers: Map<String, String>? = null,
        params: Map<String, String>? = null,
    ) = Request.Builder()
        .let { requestBuilder ->
            params?.let {
                requestBuilder.url(url.plus(if (url.contains("?")) "&" else "?")
                    .plus(params.map { it.key + "=" + it.value }.joinToString("&")))
            }
            val requestBody = RequestBody.create(JSON, requestBodyStr.ifBlank {
                "{}"
            })
            when (method) {
                "GET" -> requestBuilder.get()
                "POST" -> requestBuilder.post(requestBody)
                "DELETE" -> requestBuilder.delete(requestBody)
                "PUT" -> requestBuilder.put(requestBody)
            }
            headers?.forEach {
                requestBuilder.header(it.key, it.value)
            }
            requestBuilder.build()
        }

    fun sendRequest(
        method: String,
        url: String,
        requestBody: String,
        headers: Map<String, String>? = null,
        params: Map<String, String>? = null,
        retryCount: Int = MAX_RETRY_COUNT,
        failAction: ((exception: Exception) -> Unit) = { },
        successAction: (() -> Unit) = { }
    ) {
        val request = buildRequest(
            method = method,
            url = url,
            requestBodyStr = requestBody,
            headers = headers,
            params = params
        )
        try {
            HttpRetryUtils.retry(retryCount) {
                httpClient.newCall(request).execute()
            }
            successAction.invoke()
        } catch (ignored: Exception) {
            logger.warn("fail to send request|url[$url],$ignored")
            failAction.invoke(ignored)
        }
    }
}
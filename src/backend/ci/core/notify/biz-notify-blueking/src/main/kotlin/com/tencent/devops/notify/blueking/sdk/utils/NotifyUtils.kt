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
package com.tencent.devops.notify.blueking.sdk.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.notify.blueking.sdk.pojo.ApiReq
import com.tencent.devops.notify.blueking.sdk.pojo.ApiResp
import com.tencent.devops.notify.blueking.sdk.pojo.NotifyProperties
import com.tencent.devops.notify.constant.NotifyMessageCode.ERROR_NOTIFY_SEND_FAIL
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Suppress("ALL")
@Component
class NotifyUtils constructor(
    notifyProperties: NotifyProperties
) {
    private val logger = LoggerFactory.getLogger(NotifyUtils::class.java)
    private val appCode = notifyProperties.appCode!!
    private val appSecret = notifyProperties.appSecret!!
    private val host = notifyProperties.bkHost!!

    /**
     * 执行post请求
     */
    fun doPostRequest(uri: String, body: ApiReq): ApiResp {
        body.bk_app_code = appCode
        body.bk_app_secret = appSecret

        val jsonbody = ObjectMapper().writeValueAsString(body)
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonbody.toString())
        val url = host + uri
        logger.info("notify post url: $url")
        logger.info("notify post body: $jsonbody")

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        val result = this.doRequest(request)
        logger.info("notify post request result: $result")

        return result
    }

    // 处理请求结果
    private fun doRequest(request: Request): ApiResp {
        var resultBean = ApiResp()
        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body!!.string()
                logger.info("notify response: $responseStr")
                resultBean = ObjectMapper().readValue(responseStr, ApiResp::class.java)
            } else {
                logger.error("NOTIFY_REQUEST_FAILED|url=${request.url.toUrl()}|response=($response)")
            }
            if (!resultBean.result!!) {
                logger.error("NOTIFY_SEND_MSG_FAILED|url=${request.url.toUrl()}|message=${resultBean.message}")
            }
            return resultBean
        } catch (ignore: Exception) {
            logger.error("NOTIFY_SEND_MSG_FAILED|url=${request.url.toUrl()}|message=${ignore.message}", ignore)
            throw ErrorCodeException(
                errorCode = ERROR_NOTIFY_SEND_FAIL,
                defaultMessage = "notify send msg failed: ${ignore.message}"
            )
        }
    }

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
        }

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    })

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.SECONDS)
        .writeTimeout(30L, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private fun sslSocketFactory(): SSLSocketFactory {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            return sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}

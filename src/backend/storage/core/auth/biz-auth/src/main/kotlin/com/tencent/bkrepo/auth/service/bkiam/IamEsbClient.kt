/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.bkiam

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.auth.pojo.IamBaseReq
import com.tencent.bkrepo.auth.pojo.IamCreateApiReq
import com.tencent.bkrepo.auth.pojo.IamPermissionUrlReq
import com.tencent.bkrepo.auth.util.HttpUtils
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class IamEsbClient {
    @Value("\${esb.code:}")
    private val appCode: String = ""
    @Value("\${esb.secret:}")
    private val appSecret: String = ""
    @Value("\${esb.iam.url:}")
    private val iamUrl: String = ""

    fun createRelationResource(iamCreateApiReq: IamCreateApiReq) {
        logger.info("createRelationResource, iamCreateApiReq: $iamCreateApiReq")
        val url = buildUrl("api/c/compapi/v2/iam/authorization/resource_creator_action/")
        logger.debug("createRelationResource, url[$url]")
        val content = objectMapper.writeValueAsString(setCredentials(iamCreateApiReq, appCode, appSecret))
        logger.debug("v3 createRelationResource body[$content]")
        val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), content)
        val request = Request.Builder().url(url).post(requestBody).build()
        val apiResponse = HttpUtils.doRequest(okHttpClient, request, 1)
        val iamApiRes = objectMapper.readValue<Map<String, Any>>(apiResponse.content)
        if (iamApiRes["code"] != 0 || iamApiRes["result"] == false) {
            throw RuntimeException("esbiam request failed, response: ${apiResponse.content}")
        }
    }

    fun getPermissionUrl(iamPermissionUrl: IamPermissionUrlReq): String? {
        logger.info("getPermissionUrl, iamPermissionUrl: $iamPermissionUrl")
        val url = buildUrl("/api/c/compapi/v2/iam/application/")
        logger.info("getPermissionUrl, url:$url")
        val content = objectMapper.writeValueAsString(setCredentials(iamPermissionUrl, appCode, appSecret))
        logger.info("getPermissionUrl, content:$content")
        val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), content)
        val request = Request.Builder().url(url).post(requestBody).build()
        val apiResponse = HttpUtils.doRequest(okHttpClient, request, 1)
        val iamApiRes = objectMapper.readValue<Map<String, Any>>(apiResponse.content)
        if (iamApiRes["code"] != 0 || iamApiRes["result"] == false) {
            throw RuntimeException("esbiam request failed, response: ${apiResponse.content}")
        }
        return iamApiRes["data"].toString().substringAfter("url=").substringBeforeLast("}")
    }

    private fun setCredentials(iamBaseReq: IamBaseReq, appCode: String, appSecret: String): IamBaseReq {
        iamBaseReq.bk_app_code = appCode
        iamBaseReq.bk_app_secret = appSecret
        return iamBaseReq
    }

    private fun buildUrl(uri: String) = "${iamUrl.removeSuffix("/")}/${uri.removePrefix("/")}"

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

    private fun sslSocketFactory(): SSLSocketFactory {
        try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            return sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.SECONDS)
        .writeTimeout(30L, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .build()

    companion object {
        private val logger = LoggerFactory.getLogger(IamEsbClient::class.java)
    }
}

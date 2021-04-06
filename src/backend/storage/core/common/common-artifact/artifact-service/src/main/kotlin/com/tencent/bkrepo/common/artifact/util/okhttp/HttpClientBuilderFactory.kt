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

package com.tencent.bkrepo.common.artifact.util.okhttp

import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.disableValidationSSLSocketFactory
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.disableValidationTrustManager
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.trustAllHostname
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * OKHTTP Client工厂方法
 */
object HttpClientBuilderFactory {

    private const val DEFAULT_READ_TIMEOUT_SECONDS = 10 * 1000L
    private const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 10 * 1000L

    private val defaultClient by lazy {
        OkHttpClient.Builder()
            .sslSocketFactory(disableValidationSSLSocketFactory, disableValidationTrustManager)
            .hostnameVerifier(trustAllHostname)
            .readTimeout(DEFAULT_READ_TIMEOUT_SECONDS, TimeUnit.MILLISECONDS)
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.MILLISECONDS)
            .build()
    }

    fun create(certificate: String? = null, neverReadTimeout: Boolean = false): OkHttpClient.Builder {
        return defaultClient.newBuilder()
            .apply {
                certificate?.let {
                    val trustManager = CertTrustManager.createTrustManager(it)
                    val sslSocketFactory = CertTrustManager.createSSLSocketFactory(trustManager)
                    sslSocketFactory(sslSocketFactory, trustManager)
                }
            }.apply {
                if (neverReadTimeout) {
                    readTimeout(0, TimeUnit.MILLISECONDS)
                }
            }.apply {
                writeTimeout(0, TimeUnit.MILLISECONDS)
            }
    }
}

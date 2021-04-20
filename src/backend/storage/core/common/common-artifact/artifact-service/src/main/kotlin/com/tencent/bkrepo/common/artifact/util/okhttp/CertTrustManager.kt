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

import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * SSL证书管理器
 */
object CertTrustManager {

    private const val TLS = "TLS"
    private const val X509 = "X.509"

    val disableValidationTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // no-op
        }
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // no-op
        }
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    val trustAllHostname = HostnameVerifier { _, _ -> true }
    val disableValidationSSLSocketFactory = createSSLSocketFactory(disableValidationTrustManager)

    fun createSSLSocketFactory(certString: String): SSLSocketFactory {
        val trustManager = createTrustManager(certString)
        return createSSLSocketFactory(trustManager)
    }

    fun createSSLSocketFactory(trustManager: TrustManager): SSLSocketFactory {
        val sslContext = SSLContext.getInstance(TLS).apply { init(null, arrayOf(trustManager), null) }
        return sslContext.socketFactory
    }

    fun createTrustManager(certString: String): X509TrustManager {
        val certInputStream = certString.byteInputStream(Charsets.UTF_8)
        val certificateFactory = CertificateFactory.getInstance(X509)
        val certificateList = certificateFactory.generateCertificates(certInputStream)
        require(!certificateList.isEmpty()) { "Expected non-empty set of trusted certificates." }
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        certificateList.forEachIndexed { index, certificate ->
            keyStore.setCertificateEntry(index.toString(), certificate)
        }
        val algorithm = TrustManagerFactory.getDefaultAlgorithm()
        val trustManagerFactory = TrustManagerFactory.getInstance(algorithm).apply { init(keyStore) }
        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1) { "Unexpected default trust managers size: ${trustManagers.size}" }
        val firstTrustManager = trustManagers.first()
        check(firstTrustManager is X509TrustManager) { "Unexpected default trust managers:$firstTrustManager" }
        return firstTrustManager
    }
}

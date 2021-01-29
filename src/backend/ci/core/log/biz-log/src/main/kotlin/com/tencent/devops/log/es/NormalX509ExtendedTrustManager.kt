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

package com.tencent.devops.log.es

import org.slf4j.LoggerFactory
import java.net.Socket
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager

/**
 * An insecure [TrustManager][NormalX509ExtendedTrustManager] that trusts all X.509 certificates without any verification.
 *
 *
 * **NOTE:**
 * Never use this [NormalX509ExtendedTrustManager] in production.
 * It is purely for testing purposes, and thus it is very insecure.
 *
 * <br></br>
 * Suppressed warning: java:S4830 - "Server certificates should be verified during SSL/TLS connections"
 * This TrustManager doesn't validate certificates and should not be used at production.
 * It is just meant to be used for testing purposes and it is designed not to verify server certificates.
 */
class NormalX509ExtendedTrustManager : X509ExtendedTrustManager() {

    override fun checkClientTrusted(x509Certificates: Array<out java.security.cert.X509Certificate>, authType: String, socket: Socket) {
        if (logger.isDebugEnabled) {
            logger.debug(CLIENT_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].subjectDN)
        }
    }

    override fun checkClientTrusted(x509Certificates: Array<out java.security.cert.X509Certificate>, authType: String, sslEngine: SSLEngine) {
        if (logger.isDebugEnabled) {
            logger.debug(CLIENT_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].subjectDN)
        }
    }

    override fun checkClientTrusted(x509Certificates: Array<out java.security.cert.X509Certificate>, authType: String) {
        if (logger.isDebugEnabled) {
            logger.debug(CLIENT_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].subjectDN)
        }
    }

    override fun checkServerTrusted(x509Certificates: Array<out java.security.cert.X509Certificate>, authType: String, socket: Socket) {
        if (logger.isDebugEnabled) {
            logger.debug(SERVER_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].subjectDN)
        }
    }

    override fun checkServerTrusted(x509Certificates: Array<out java.security.cert.X509Certificate>, authType: String, sslEngine: SSLEngine) {
        if (logger.isDebugEnabled) {
            logger.debug(SERVER_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].subjectDN)
        }
    }

    override fun checkServerTrusted(x509Certificates: Array<out java.security.cert.X509Certificate>, authType: String) {
        if (logger.isDebugEnabled) {
            logger.debug(SERVER_CERTIFICATE_LOG_MESSAGE, x509Certificates[0].subjectDN)
        }
    }

    override fun getAcceptedIssuers(): Array<out java.security.cert.X509Certificate> {
        return emptyArray()
    }

    companion object {
        val INSTANCE = NormalX509ExtendedTrustManager()
        private val logger = LoggerFactory.getLogger(NormalX509ExtendedTrustManager::class.java)
        private const val CLIENT_CERTIFICATE_LOG_MESSAGE = "Accepting a client certificate: [{}]"
        private const val SERVER_CERTIFICATE_LOG_MESSAGE = "Accepting a server certificate: [{}]"
    }
}

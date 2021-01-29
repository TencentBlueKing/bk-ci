package com.tencent.devops.log.es

import org.slf4j.LoggerFactory
import java.net.Socket
import javax.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager

/**
 * An insecure [TrustManager][UnsafeX509ExtendedTrustManager] that trusts all X.509 certificates without any verification.
 *
 *
 * **NOTE:**
 * Never use this [UnsafeX509ExtendedTrustManager] in production.
 * It is purely for testing purposes, and thus it is very insecure.
 *
 * <br></br>
 * Suppressed warning: java:S4830 - "Server certificates should be verified during SSL/TLS connections"
 * This TrustManager doesn't validate certificates and should not be used at production.
 * It is just meant to be used for testing purposes and it is designed not to verify server certificates.
 */
class UnsafeX509ExtendedTrustManager : X509ExtendedTrustManager() {

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
        val INSTANCE = UnsafeX509ExtendedTrustManager()
        private val logger = LoggerFactory.getLogger(UnsafeX509ExtendedTrustManager::class.java)
        private const val CLIENT_CERTIFICATE_LOG_MESSAGE = "Accepting a client certificate: [{}]"
        private const val SERVER_CERTIFICATE_LOG_MESSAGE = "Accepting a server certificate: [{}]"
    }
}

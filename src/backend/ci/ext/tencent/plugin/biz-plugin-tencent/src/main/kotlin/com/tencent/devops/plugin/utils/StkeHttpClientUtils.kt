package com.tencent.devops.plugin.utils

import com.tencent.devops.common.api.util.AESUtil
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.xml.bind.DatatypeConverter

object StkeHttpClientUtils {

    private const val key = "bk_ci_cert_AESU_key"

    private val logger = LoggerFactory.getLogger(StkeHttpClientUtils::class.java)

    fun getHttpClient(certPem: String?, certKeyPem: String?): OkHttpClient {

        if (certKeyPem == null || certPem == null) {
            logger.error("cert_pem/cert_key_pem can not find")
            throw RuntimeException("STKE plugin cert_pem/cert_key_pem can not find")
        }

        val certBytes = DatatypeConverter.parseBase64Binary(AESUtil.decrypt(key = key, content = certPem))
        val keyBytes = DatatypeConverter.parseBase64Binary(AESUtil.decrypt(key = key, content = certKeyPem))

        val cert = generateCertificateFromDER(certBytes)
        val key = generatePrivateKeyFromDER(keyBytes)

        val keystore = KeyStore.getInstance("JKS")
        keystore.load(null)
        keystore.setCertificateEntry("cert", cert)
        keystore.setKeyEntry("key", key, "cert_key".toCharArray(), Array(1) { cert })

        val kmf = KeyManagerFactory.getInstance("SunX509")
        kmf.init(keystore, "cert_key".toCharArray())

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, getTrustManager(), null)

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, getTrustManager()[0] as X509TrustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .build()
    }

    private fun generateCertificateFromDER(certBytes: ByteArray): X509Certificate {
        val factory: CertificateFactory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
    }

    private fun generatePrivateKeyFromDER(keyBytes: ByteArray): RSAPrivateKey {
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val factory: KeyFactory = KeyFactory.getInstance("RSA")
        return factory.generatePrivate(spec) as RSAPrivateKey
    }

    // 获取空的TrustManager
    private fun getTrustManager(): Array<TrustManager> {
        return arrayOf(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )
    }
}
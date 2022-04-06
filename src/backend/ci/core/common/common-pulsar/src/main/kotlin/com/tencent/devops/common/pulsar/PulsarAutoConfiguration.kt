package com.tencent.devops.common.pulsar

import com.tencent.devops.common.pulsar.properties.PulsarProperties
import org.apache.pulsar.client.api.AuthenticationFactory
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.auth.oauth2.AuthenticationFactoryOAuth2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.net.URL
import java.util.concurrent.TimeUnit

@Configuration
@ComponentScan
@EnableConfigurationProperties(PulsarProperties::class)
class PulsarAutoConfiguration @Autowired constructor(
    private val pulsarProperties: PulsarProperties
) {

    @Bean
    @ConditionalOnMissingBean
    fun pulsarClient(): PulsarClient {
        if (!pulsarProperties.tlsAuthCertFilePath.isNullOrBlank() &&
            !pulsarProperties.tlsAuthKeyFilePath.isNullOrBlank() &&
            !pulsarProperties.tokenAuthValue.isNullOrBlank())
            throw IllegalArgumentException("You cannot use multiple auth options.")
        val pulsarClientBuilder = PulsarClient.builder()
            .serviceUrl(pulsarProperties.serviceUrl)
            .ioThreads(pulsarProperties.ioThreads)
            .listenerThreads(pulsarProperties.listenerThreads)
            .enableTcpNoDelay(pulsarProperties.isEnableTcpNoDelay)
            .keepAliveInterval(pulsarProperties.keepAliveIntervalSec, TimeUnit.SECONDS)
            .connectionTimeout(pulsarProperties.connectionTimeoutSec, TimeUnit.SECONDS)
            .operationTimeout(pulsarProperties.operationTimeoutSec, TimeUnit.SECONDS)
            .startingBackoffInterval(pulsarProperties.startingBackoffIntervalMs, TimeUnit.MILLISECONDS)
            .maxBackoffInterval(pulsarProperties.maxBackoffIntervalSec, TimeUnit.SECONDS)
            .useKeyStoreTls(pulsarProperties.useKeyStoreTls)
            .tlsTrustCertsFilePath(pulsarProperties.tlsTrustCertsFilePath)
            .tlsCiphers(pulsarProperties.tlsCiphers)
            .tlsProtocols(pulsarProperties.tlsProtocols)
            .tlsTrustStorePassword(pulsarProperties.tlsTrustStorePassword)
            .tlsTrustStorePath(pulsarProperties.tlsTrustStorePath)
            .tlsTrustStoreType(pulsarProperties.tlsTrustStoreType)
            .allowTlsInsecureConnection(pulsarProperties.allowTlsInsecureConnection)
            .enableTlsHostnameVerification(pulsarProperties.enableTlsHostnameVerification)
        if (!pulsarProperties.tlsAuthCertFilePath.isNullOrBlank() &&
            !pulsarProperties.tlsAuthKeyFilePath.isNullOrBlank()) {
            pulsarClientBuilder.authentication(
                AuthenticationFactory
                    .TLS(pulsarProperties.tlsAuthCertFilePath, pulsarProperties.tlsAuthKeyFilePath)
            )
        }
        if (!pulsarProperties.tokenAuthValue.isNullOrBlank()) {
            pulsarClientBuilder.authentication(
                AuthenticationFactory
                    .token(pulsarProperties.tokenAuthValue)
            )
        }
        if (!pulsarProperties.oauth2Audience.isNullOrBlank() &&
            !pulsarProperties.oauth2IssuerUrl.isNullOrBlank() &&
            !pulsarProperties.oauth2CredentialsUrl.isNullOrBlank()) {
            val issuerUrl = URL(pulsarProperties.oauth2IssuerUrl)
            val credentialsUrl = URL(pulsarProperties.oauth2CredentialsUrl)
            pulsarClientBuilder.authentication(
                AuthenticationFactoryOAuth2
                    .clientCredentials(issuerUrl, credentialsUrl, pulsarProperties.oauth2Audience)
            )
        }
        return pulsarClientBuilder.build()
    }
}

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.stream.binder.pulsar.autoconfigurate

import com.tencent.bkrepo.common.stream.binder.pulsar.PulsarMessageChannelBinder
import com.tencent.bkrepo.common.stream.binder.pulsar.error.exception.ClientInitException
import com.tencent.bkrepo.common.stream.binder.pulsar.properties.PulsarBinderConfigurationProperties
import com.tencent.bkrepo.common.stream.binder.pulsar.properties.PulsarExtendedBindingProperties
import com.tencent.bkrepo.common.stream.binder.pulsar.provisioning.PulsarMessageQueueProvisioner
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.AuthenticationFactory
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException
import org.apache.pulsar.client.impl.auth.oauth2.AuthenticationFactoryOAuth2
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PulsarExtendedBindingProperties::class, PulsarBinderConfigurationProperties::class)
class PulsarBinderAutoConfiguration {

    @Bean
    fun pulsarMessageQueueProvisioner(): PulsarMessageQueueProvisioner {
        return PulsarMessageQueueProvisioner()
    }

    @Bean
    @ConditionalOnMissingBean
    @Throws(PulsarClientException::class, ClientInitException::class, MalformedURLException::class)
    fun pulsarClient(pulsarProperties: PulsarBinderConfigurationProperties): PulsarClient {
        if (!pulsarProperties.tlsAuthCertFilePath.isNullOrEmpty() &&
            !pulsarProperties.tlsAuthKeyFilePath.isNullOrEmpty() &&
            !pulsarProperties.tokenAuthValue.isNullOrEmpty()
        ) throw ClientInitException("You cannot use multiple auth options.")
        val pulsarClientBuilder = PulsarClient.builder()
            .serviceUrl(pulsarProperties.serviceUrl)
            .ioThreads(pulsarProperties.ioThreads)
            .listenerThreads(pulsarProperties.listenerThreads)
            .enableTcpNoDelay(pulsarProperties.enableTcpNoDelay)
            .keepAliveInterval(pulsarProperties.keepAliveIntervalSec, TimeUnit.SECONDS)
            .connectionTimeout(pulsarProperties.connectionTimeoutSec, TimeUnit.SECONDS)
            .operationTimeout(pulsarProperties.operationTimeoutSec, TimeUnit.SECONDS)
            .startingBackoffInterval(pulsarProperties.startingBackoffIntervalMs.toLong(), TimeUnit.MILLISECONDS)
            .maxBackoffInterval(pulsarProperties.maxBackoffIntervalSec.toLong(), TimeUnit.SECONDS)
            .useKeyStoreTls(pulsarProperties.useKeyStoreTls)
            .tlsTrustCertsFilePath(pulsarProperties.tlsTrustCertsFilePath)
            .tlsCiphers(pulsarProperties.tlsCiphers)
            .tlsProtocols(pulsarProperties.tlsProtocols)
            .tlsTrustStorePassword(pulsarProperties.tlsTrustStorePassword)
            .tlsTrustStorePath(pulsarProperties.tlsTrustStorePath)
            .tlsTrustStoreType(pulsarProperties.tlsTrustStoreType)
            .allowTlsInsecureConnection(pulsarProperties.allowTlsInsecureConnection)
            .enableTlsHostnameVerification(pulsarProperties.enableTlsHostnameVerification)

        if (!pulsarProperties.tlsAuthCertFilePath.isNullOrEmpty() &&
            !pulsarProperties.tlsAuthKeyFilePath.isNullOrEmpty()
        ) {
            pulsarClientBuilder.authentication(
                AuthenticationFactory
                    .TLS(pulsarProperties.tlsAuthCertFilePath, pulsarProperties.tlsAuthKeyFilePath)
            )
        }

        if (!pulsarProperties.tokenAuthValue.isNullOrEmpty()) {
            pulsarClientBuilder.authentication(
                AuthenticationFactory
                    .token(pulsarProperties.tokenAuthValue)
            )
        }

        if (!pulsarProperties.oauth2Audience.isNullOrEmpty() &&
            !pulsarProperties.oauth2IssuerUrl.isNullOrEmpty() &&
            !pulsarProperties.oauth2CredentialsUrl.isNullOrEmpty()
        ) {
            val issuerUrl = URL(pulsarProperties.oauth2IssuerUrl)
            val credentialsUrl = URL(pulsarProperties.oauth2CredentialsUrl)
            pulsarClientBuilder.authentication(
                AuthenticationFactoryOAuth2
                    .clientCredentials(issuerUrl, credentialsUrl, pulsarProperties.oauth2Audience)
            )
        }
        return pulsarClientBuilder.build()
    }

    @Bean
    fun pulsarMessageChannelBinder(
        pulsarClient: PulsarClient,
        provisioningProvider: PulsarMessageQueueProvisioner,
        bindingProperties: PulsarExtendedBindingProperties,
        pulsarProperties: PulsarBinderConfigurationProperties
    ): PulsarMessageChannelBinder {
        return PulsarMessageChannelBinder(
            pulsarClient,
            provisioningProvider,
            bindingProperties,
            pulsarProperties
        )
    }

//    @Configuration(proxyBeanMethods = false)
//    @ConditionalOnClass(HealthIndicator::class)
//    @ConditionalOnEnabledHealthIndicator("pulsar")
//    internal class PulsarBinderHealthIndicatorConfiguration {
//        @Bean
//        fun pulsarBinderHealthIndicator(): PulsarBinderHealthIndicator {
//            return PulsarBinderHealthIndicator()
//        }
//    }
}

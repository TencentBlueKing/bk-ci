/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
 *
 */

package com.tencent.devops.common.auth.rbac

import com.tencent.bk.sdk.iam.util.http.AuthUrlMapper
import com.tencent.bk.sdk.iam.util.http.DefaultApacheHttpClientBuilder.IdleConnectionMonitorThread
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.httpcomponents.MicrometerHttpRequestExecutor
import io.micrometer.core.instrument.binder.httpcomponents.PoolingHttpClientConnectionManagerMetricsBinder
import org.apache.http.client.config.RequestConfig
import org.apache.http.config.RegistryBuilder
import org.apache.http.config.SocketConfig
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.ssl.SSLContexts
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException

@Suppress("ALL")
@Configuration
@EnableConfigurationProperties(RbacAuthHttpClientProperties::class)
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class RbacAuthHttpClientAutoConfiguration(
    private val httpClientProperties: RbacAuthHttpClientProperties
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacAuthHttpClientAutoConfiguration::class.java)
    }

    @Bean
    fun poolingConnectionManager(meterRegistry: MeterRegistry): PoolingHttpClientConnectionManager {
        val sslConnectionSocketFactory = SSLConnectionSocketFactory.getSocketFactory()
        val plainConnectionSocketFactory = PlainConnectionSocketFactory.getSocketFactory()

        val registry = RegistryBuilder.create<ConnectionSocketFactory>()
            .register("http", plainConnectionSocketFactory)
            .register("https", sslConnectionSocketFactory)
            .build()

        val connectionManager = PoolingHttpClientConnectionManager(registry)
        connectionManager.maxTotal = httpClientProperties.maxTotalConn
        connectionManager.defaultMaxPerRoute = httpClientProperties.maxConnPerHost
        connectionManager.defaultSocketConfig = SocketConfig.copy(SocketConfig.DEFAULT)
            .setSoTimeout(httpClientProperties.soTimeout)
            .build()

        val idleConnectionMonitorThread = IdleConnectionMonitorThread(
            connectionManager, httpClientProperties.idleConnTimeout, httpClientProperties.checkWaitTime
        )
        idleConnectionMonitorThread.isDaemon = true
        idleConnectionMonitorThread.start()

        // 配置连接池监控
        PoolingHttpClientConnectionManagerMetricsBinder(
            connectionManager, "auth-http-client-pool"
        ).bindTo(meterRegistry)
        return connectionManager
    }

    @Bean
    fun httpClient(
        poolingConnectionManager: PoolingHttpClientConnectionManager,
        meterRegistry: MeterRegistry
    ): CloseableHttpClient {
        val httpClientBuilder = HttpClients.custom()
            .setConnectionManager(poolingConnectionManager)
            .setConnectionManagerShared(true)
            .setSSLSocketFactory(buildSSLConnectionSocketFactory())
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setSocketTimeout(httpClientProperties.soTimeout)
                    .setConnectTimeout(httpClientProperties.connectionTimeout)
                    .setConnectionRequestTimeout(httpClientProperties.connectionRequestTimeout)
                    .build()
            )
            .setRequestExecutor(
                MicrometerHttpRequestExecutor
                    .builder(meterRegistry)
                    .uriMapper(AuthUrlMapper())
                    .build()
            )
        return httpClientBuilder.build()
    }

    private fun buildSSLConnectionSocketFactory(): SSLConnectionSocketFactory? {
        try {
            val sslContext = SSLContexts.custom() // 忽略掉对服务器端证书的校验
                .loadTrustMaterial(TrustStrategy { _, _ -> true }).build()
            return SSLConnectionSocketFactory(
                sslContext, arrayOf("TLSv1"),
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier()
            )
        } catch (e: NoSuchAlgorithmException) {
            logger.error(e.message, e)
        } catch (e: KeyManagementException) {
            logger.error(e.message, e)
        } catch (e: KeyStoreException) {
            logger.error(e.message, e)
        }
        return null
    }
}

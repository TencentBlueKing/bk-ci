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

package com.tencent.devops.common.es

import com.floragunn.searchguard.ssl.SearchGuardSSLPlugin
import com.floragunn.searchguard.ssl.util.SSLConfigConstants
import com.tencent.devops.common.web.WebAutoConfiguration
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import java.net.InetAddress

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(WebAutoConfiguration::class)
@EnableConfigurationProperties(ESProperties::class)
class ESAutoConfiguration {
    @Value("\${elasticsearch.ip}")
    private val ip: String? = null
    @Value("\${elasticsearch.port}")
    private val port: Int? = 0
    @Value("\${elasticsearch.cluster:#{null}}")
    private val cluster: String? = null
    @Value("\${elasticsearch.keystore.filePath:#{null}}")
    private val keystoreFilePath: String? = null
    @Value("\${elasticsearch.keystore.password:#{null}}")
    private val keystorePassword: String? = null
    @Value("\${elasticsearch.truststore.filePath:#{null}}")
    private val truststoreFilePath: String? = null
    @Value("\${elasticsearch.truststore.password:#{null}}")
    private val truststorePassword: String? = null

    @Bean
    @Primary
    fun transportClient(): TransportClient {
        if (ip.isNullOrBlank()) {
            throw IllegalArgumentException("ES集群地址尚未配置")
        }
        if (port == null || port!! <= 0) {
            throw IllegalArgumentException("ES集群端口尚未配置")
        }

        val builder = Settings.builder()

        var plugin: Class<out Plugin>? = null

        if (!cluster.isNullOrBlank()) {
            builder.put("cluster.name", cluster)
        }

        if (!keystoreFilePath.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_KEYSTORE_FILEPATH, keystoreFilePath)
                .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION, false)
                .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_ENABLED, true)
                .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION_RESOLVE_HOST_NAME, true)
        }
        if (!truststoreFilePath.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_TRUSTSTORE_FILEPATH, truststoreFilePath)
        }
        if (!keystorePassword.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_KEYSTORE_PASSWORD, keystorePassword)
        }
        if (!truststorePassword.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_TRUSTSTORE_PASSWORD, truststorePassword)
            plugin = SearchGuardSSLPlugin::class.java
        }

        val settings = builder.build()
        val ips = ip!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val client = if (plugin != null) {
            PreBuiltTransportClient(settings, plugin)
        } else {
            PreBuiltTransportClient(settings)
        }
        for (ipAddress in ips) {
            client.addTransportAddress(InetSocketTransportAddress(InetAddress.getByName(ipAddress), port!!))
        }
        return client
    }
}

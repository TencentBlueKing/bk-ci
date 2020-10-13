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

package com.tencent.devops.log.configuration

import com.floragunn.searchguard.ssl.SearchGuardSSLPlugin
import com.floragunn.searchguard.ssl.util.SSLConfigConstants
import com.tencent.devops.common.es.ESClient
import com.tencent.devops.common.es.ESProperties
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.WebAutoConfiguration
import com.tencent.devops.log.client.impl.MultiESLogClient
import com.tencent.devops.log.dao.TencentIndexDao
import com.tencent.devops.log.dao.IndexDao
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import java.net.InetAddress

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@AutoConfigureBefore(WebAutoConfiguration::class)
@AutoConfigureAfter(LogClientConfiguration::class)
@EnableConfigurationProperties(ESProperties::class)
class LogESAutoConfiguration {
    @Value("\${elasticsearch.ip}")
    private val e1IP: String? = null
    @Value("\${elasticsearch.port}")
    private val e1Port: Int? = 0
    @Value("\${elasticsearch.cluster}")
    private val e1Cluster: String? = null
    @Value("\${elasticsearch.keystore.filePath:#{null}}")
    private val e1KeystoreFilePath: String? = null
    @Value("\${elasticsearch.keystore.password:#{null}}")
    private val e1KeystorePassword: String? = null
    @Value("\${elasticsearch.truststore.filePath:#{null}}")
    private val e1TruststoreFilePath: String? = null
    @Value("\${elasticsearch.truststore.password:#{null}}")
    private val e1TruststorePassword: String? = null
    @Value("\${elasticsearch.name}")
    private val e1Name: String? = null
    @Value("\${elasticsearch.mainCluster:#{null}}")
    private val e1MainCluster: String? = null

    @Value("\${elasticsearch2.ip}")
    private val e2IP: String? = null
    @Value("\${elasticsearch2.port}")
    private val e2Port: Int? = 0
    @Value("\${elasticsearch2.cluster}")
    private val e2Cluster: String? = null
    @Value("\${elasticsearch2.keystore.filePath:#{null}}")
    private val e2KeystoreFilePath: String? = null
    @Value("\${elasticsearch2.keystore.password:#{null}}")
    private val e2KeystorePassword: String? = null
    @Value("\${elasticsearch2.truststore.filePath:#{null}}")
    private val e2TruststoreFilePath: String? = null
    @Value("\${elasticsearch2.truststore.password:#{null}}")
    private val e2TruststorePassword: String? = null
    @Value("\${elasticsearch2.name}")
    private val e2Name: String? = null
    @Value("\${elasticsearch2.mainCluster:#{null}}")
    private val e2MainCluster: String? = null

    fun client(): ESClient {
        if (e1IP.isNullOrBlank()) {
            throw IllegalArgumentException("ES集群地址尚未配置")
        }
        if (e1Port == null || e1Port!! <= 0) {
            throw IllegalArgumentException("ES集群端口尚未配置")
        }
        if (e1Cluster.isNullOrBlank()) {
            throw IllegalArgumentException("ES集群名称尚未配置")
        }
        if (e1Name.isNullOrBlank()) {
            throw IllegalArgumentException("ES唯一名称尚未配置")
        }

        val builder = Settings.builder().put("cluster.name", e1Cluster)
        var plugin: Class<out Plugin>? = null

        if (!e1KeystoreFilePath.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_KEYSTORE_FILEPATH, e1KeystoreFilePath)
                .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION, false)
                .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_ENABLED, true)
                .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION_RESOLVE_HOST_NAME, true)
        }
        if (!e1TruststoreFilePath.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_TRUSTSTORE_FILEPATH, e1TruststoreFilePath)
        }
        if (!e1KeystorePassword.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_KEYSTORE_PASSWORD, e1KeystorePassword)
        }
        if (!e1TruststorePassword.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_TRUSTSTORE_PASSWORD, e1TruststorePassword)
            plugin = SearchGuardSSLPlugin::class.java
        }
        val settings = builder.build()
        val ips = e1IP!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val client = if (plugin != null) {
            PreBuiltTransportClient(settings, plugin)
        } else {
            PreBuiltTransportClient(settings)
        }
        for (ipAddress in ips) {
            client.addTransportAddress(InetSocketTransportAddress(InetAddress.getByName(ipAddress), e1Port!!))
        }
        val mainCluster = if (!e1MainCluster.isNullOrBlank()) {
            e1MainCluster!!.toBoolean()
        } else {
            false
        }
        logger.info("Init the log es1 transport client with host($e1Name:$mainCluster:$e1IP:$e1Port), cluster($e1Cluster), keystore($e1KeystoreFilePath|$e1KeystorePassword), truststore($e1TruststoreFilePath|$e1TruststorePassword)")
        return ESClient(e1Name!!, client, mainCluster)
    }

    fun client2(): ESClient {
        if (e2IP.isNullOrBlank()) {
            throw IllegalArgumentException("ES2集群地址尚未配置")
        }
        if (e2Port == null || e2Port!! <= 0) {
            throw IllegalArgumentException("ES2集群端口尚未配置")
        }
        if (e2Cluster.isNullOrBlank()) {
            throw IllegalArgumentException("ES2集群名称尚未配置")
        }

        if (e2Name.isNullOrBlank()) {
            throw IllegalArgumentException("ES2唯一名称尚未配置")
        }

        val builder = Settings.builder().put("cluster.name", e2Cluster)

        var plugin: Class<out Plugin>? = null

        if (!e2KeystoreFilePath.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_KEYSTORE_FILEPATH, e2KeystoreFilePath)
                    .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION, false)
                    .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_ENABLED, true)
                    .put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION_RESOLVE_HOST_NAME, true)
        }
        if (!e2TruststoreFilePath.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_TRUSTSTORE_FILEPATH, e2TruststoreFilePath)
        }
        if (!e2KeystorePassword.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_KEYSTORE_PASSWORD, e2KeystorePassword)
        }
        if (!e2TruststorePassword.isNullOrBlank()) {
            builder.put(SSLConfigConstants.SEARCHGUARD_SSL_TRANSPORT_TRUSTSTORE_PASSWORD, e2TruststorePassword)
            plugin = SearchGuardSSLPlugin::class.java
        }

        val settings = builder.build()
        val ips = e2IP!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val client = if (plugin != null) {
            PreBuiltTransportClient(settings, plugin)
        } else {
            PreBuiltTransportClient(settings)
        }
        for (ipAddress in ips) {
            client.addTransportAddress(InetSocketTransportAddress(InetAddress.getByName(ipAddress), e2Port!!))
        }
        val mainCluster = if (!e2MainCluster.isNullOrBlank()) {
            e2MainCluster!!.toBoolean()
        } else {
            false
        }
        logger.info("Init the log es2 transport client with host($e2Name:$mainCluster:$e2IP:$e2Port), cluster($e2Cluster), keystore($e2KeystoreFilePath|$e2KeystorePassword), truststore($e2TruststoreFilePath|$e2TruststorePassword)")
        return ESClient(e2Name!!, client, mainCluster)
    }

    @Bean
    fun logClient(
        @Autowired redisOperation: RedisOperation,
        @Autowired tencentIndexDao: TencentIndexDao,
        @Autowired indexDao: IndexDao,
        @Autowired dslContext: DSLContext
    ) = MultiESLogClient(listOf(client(), client2()), redisOperation, dslContext, tencentIndexDao, indexDao)

    companion object {
        private val logger = LoggerFactory.getLogger(LogESAutoConfiguration::class.java)
    }
}

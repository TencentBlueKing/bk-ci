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

import com.tencent.devops.common.es.ESProperties
import com.tencent.devops.common.web.WebAutoConfiguration
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import java.net.InetAddress
import java.util.Base64

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(WebAutoConfiguration::class)
@EnableConfigurationProperties(ESProperties::class)
class LogESAutoConfiguration {
    @Value("\${elasticsearch.ip}")
    private val e1IP: String? = null
    @Value("\${elasticsearch.port}")
    private val e1Port: Int? = 0
    @Value("\${elasticsearch.cluster}")
    private val e1Cluster: String? = null

    @Value("\${elasticsearch2.ip}")
    private val e2IP: String? = null
    @Value("\${elasticsearch2.port}")
    private val e2Port: Int? = 0
    @Value("\${elasticsearch2.cluster}")
    private val e2Cluster: String? = null
    @Value("\${elasticsearch2.username}")
    private val e2Username: String? = null
    @Value("\${elasticsearch2.password}")
    private val e2Password: String? = null

    @Bean
    @Primary
    fun client(): Client {
        if (e1IP.isNullOrBlank()) {
            throw IllegalArgumentException("ES集群地址尚未配置")
        }
        if (e1Port == null || e1Port!! <= 0) {
            throw IllegalArgumentException("ES集群端口尚未配置")
        }
        if (e1Cluster.isNullOrBlank()) {
            throw IllegalArgumentException("ES集群名称尚未配置")
        }
        val settings = Settings.builder().put("cluster.name", e1Cluster).build()
        val ips = e1IP!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val client = PreBuiltTransportClient(settings)
        for (ipAddress in ips) {
            client.addTransportAddress(InetSocketTransportAddress(InetAddress.getByName(ipAddress), e1Port!!))
        }
        logger.info("Init ES transport client with host($e1IP:$e1Port) and cluster($e1Cluster)")
        return client
    }

    @Bean
    fun client2(): Client {
        if (e2IP.isNullOrBlank()) {
            throw IllegalArgumentException("ES2集群地址尚未配置")
        }
        if (e2Port == null || e2Port!! <= 0) {
            throw IllegalArgumentException("ES2集群端口尚未配置")
        }
        if (e2Cluster.isNullOrBlank()) {
            throw IllegalArgumentException("ES2集群名称尚未配置")
        }
        if (e2Username.isNullOrBlank()) {
            throw IllegalArgumentException("ES2用户名尚未配置")
        }
        if (e2Password.isNullOrBlank()) {
            throw IllegalArgumentException("ES2密码尚未配置")
        }
        val settings = Settings.builder().put("cluster.name", e2Cluster).build()
        val ips = e2IP!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val client = PreBuiltTransportClient(settings)
        for (ipAddress in ips) {
            client.addTransportAddress(InetSocketTransportAddress(InetAddress.getByName(ipAddress), e2Port!!))
        }
        val auth = Base64.getEncoder().encode(("$e2Username:$e2Password").toByteArray()).toString(Charsets.UTF_8)
        logger.info("Init ES 2 transport client with host($e2IP:$e2Port) and cluster($e2Cluster)")
        return client.filterWithHeader(mapOf("Authorization" to "Basic $auth"))
    }
    companion object {
        private val logger = LoggerFactory.getLogger(LogESAutoConfiguration::class.java)
    }
}

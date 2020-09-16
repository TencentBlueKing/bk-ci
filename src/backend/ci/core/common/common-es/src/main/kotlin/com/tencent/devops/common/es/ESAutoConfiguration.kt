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

import com.tencent.devops.common.web.WebAutoConfiguration
import org.apache.http.HttpHost
import org.apache.http.ssl.SSLContexts
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(WebAutoConfiguration::class)
@EnableConfigurationProperties(ESProperties::class)
class ESAutoConfiguration : DisposableBean {
    @Value("\${elasticsearch.ip}")
    private val ip: String? = null
    @Value("\${elasticsearch.port}")
    private val port: Int? = 9200
    @Value("\${elasticsearch.cluster}")
    private val cluster: String? = null
    @Value("\${elasticsearch.name}")
    private val name: String? = null
    @Value("\${elasticsearch.keystore.filePath:#{null}}")
    private val keystoreFilePath: String? = null
    @Value("\${elasticsearch.keystore.password:#{null}}")
    private val keystorePassword: String? = null
    @Value("\${elasticsearch.truststore.filePath:#{null}}")
    private val truststoreFilePath: String? = null
    @Value("\${elasticsearch.truststore.password:#{null}}")
    private val truststorePassword: String? = null

    private var client: RestHighLevelClient? = null

    @Bean
    @Primary
    fun transportClient(): ESClient {
        if (ip.isNullOrBlank()) {
            throw IllegalArgumentException("ES集群地址尚未配置: elasticsearch.ip")
        }
        if (port == null || port!! <= 0) {
            throw IllegalArgumentException("ES集群端口尚未配置: elasticsearch.port")
        }
        if (cluster.isNullOrBlank()) {
            throw IllegalArgumentException("ES集群名称尚未配置: elasticsearch.cluster")
        }
        if (name.isNullOrBlank()) {
            throw IllegalArgumentException("ES唯一名称尚未配置: elasticsearch.name")
        }
        val esPort = port ?: 9200
        val searchGuard =
            !keystoreFilePath.isNullOrBlank() || !truststoreFilePath.isNullOrBlank() ||
                !keystorePassword.isNullOrBlank() || !truststorePassword.isNullOrBlank()
        client = if (searchGuard) {
            if (keystoreFilePath.isNullOrBlank()) {
                throw IllegalArgumentException("SearchGuard认证缺少配置: elasticsearch.keystore.filePath")
            }
            if (truststoreFilePath.isNullOrBlank()) {
                throw IllegalArgumentException("SearchGuard认证缺少配置: elasticsearch.keystore.password")
            }
            if (keystorePassword.isNullOrBlank()) {
                throw IllegalArgumentException("SearchGuard认证缺少配置: elasticsearch.truststore.filePath")
            }
            if (truststorePassword.isNullOrBlank()) {
                throw IllegalArgumentException("SearchGuard认证缺少配置: elasticsearch.truststore.password")
            }

            val truststore = KeyStore.getInstance("jks")
            val inputStream = FileInputStream(File(keystoreFilePath!!))
            truststore.load(inputStream, keystorePassword!!.toCharArray())
            val sslBuilder = SSLContexts.custom().loadTrustMaterial(truststore, null)
            val sslContext = sslBuilder.build()

            val builder = RestClient.builder(HttpHost(ip, esPort, "http"))
                .setHttpClientConfigCallback { httpClientBuilder ->
                    httpClientBuilder.setSSLContext(sslContext)
                    httpClientBuilder
                }
            RestHighLevelClient(builder)
        } else {
            RestHighLevelClient(
                RestClient.builder(HttpHost(ip, esPort, "http"))
            )
        }
        return ESClient(name!!, client!!)
    }

    override fun destroy() {
        client?.close()
    }
}

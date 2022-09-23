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

package com.tencent.devops.log.es

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.WebAutoConfiguration
import com.tencent.devops.log.client.LogClient
import com.tencent.devops.log.client.impl.LogClientImpl
import com.tencent.devops.log.jmx.CreateIndexBean
import com.tencent.devops.log.jmx.LogStorageBean
import com.tencent.devops.log.service.BuildLogPrintService
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.LogService
import com.tencent.devops.log.service.LogStatusService
import com.tencent.devops.log.service.LogTagService
import com.tencent.devops.log.service.impl.LogServiceESImpl
import com.tencent.devops.log.util.ESConfigUtils
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.ssl.SSLContexts
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext

@Suppress("ALL")
@Configuration
@ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "elasticsearch")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(WebAutoConfiguration::class)
@EnableConfigurationProperties(ESProperties::class)
class ESAutoConfiguration : DisposableBean {
    @Value("\${log.elasticsearch.ip}")
    private val host: String? = null
    @Value("\${log.elasticsearch.port}")
    private val port: Int? = null

    @Value("\${log.elasticsearch.cluster}")
    private val cluster: String? = null

    @Value("\${log.elasticsearch.name}")
    private val name: String? = null

    @Value("\${log.elasticsearch.username:#{null}}")
    private val username: String? = null

    @Value("\${log.elasticsearch.password:#{null}}")
    private val password: String? = null

    @Value("\${log.elasticsearch.https:#{null}}")
    private val https: String? = null

    @Value("\${log.elasticsearch.keystore.filePath:#{null}}")
    private val keystoreFilePath: String? = null

    @Value("\${log.elasticsearch.keystore.password:#{null}}")
    private val keystorePassword: String? = null

    @Value("\${log.elasticsearch.truststore.filePath:#{null}}")
    private val truststoreFilePath: String? = null

    @Value("\${log.elasticsearch.truststore.password:#{null}}")
    private val truststorePassword: String? = null

    @Value("\${log.elasticsearch.shards:#{null}}")
    private val shards: Int? = null

    @Value("\${log.elasticsearch.replicas:#{null}}")
    private val replicas: Int? = null

    @Value("\${log.elasticsearch.shardsPerNode:#{null}}")
    private val shardsPerNode: Int? = null

    @Value("\${log.elasticsearch.socketTimeout:#{null}}")
    private val socketTimeout: Int? = null

    private var client: RestHighLevelClient? = null

    @Bean
    @Primary
    fun transportClient(): ESClient {
        if (host.isNullOrBlank()) {
            throw IllegalArgumentException("ip of elasticsearch not config: log.elasticsearch.ip")
        }
        if (cluster.isNullOrBlank()) {
            throw IllegalArgumentException("cluster of elasticsearch not config: log.elasticsearch.cluster")
        }
        if (name.isNullOrBlank()) {
            throw IllegalArgumentException("name of elasticsearch not config: log.elasticsearch.name")
        }

        val httpPort = port ?: 9200
        val indexShards = shards ?: 1 // 索引总分片数
        val indexReplicas = replicas ?: 1 // 分片副本数
        val indexShardsPerNode = shardsPerNode ?: 1 // 每个节点分片数
        val socketTimeout = socketTimeout ?: 30000 // 等待连接响应超时
        val tcpKeepAliveSeconds = 30000 // 探活连接时长
        val connectTimeOut = 1000 // 请求连接超时
        val connectionRequestTimeOut = 500 // 获取连接的超时时间
        val maxConnectNum = 100 // 最大连接数
        val maxConnectPerRoute = 30 // 最大路由连接数
        val requestTimeout = if (socketTimeout > 0) { // ES响应超时，取主动超时的一半
            socketTimeout / 2
        } else {
            30000
        }

        var sslContext: SSLContext? = null

        // 基础鉴权 - 账号密码
        val credentialsProvider = if (!username.isNullOrBlank() || !password.isNullOrBlank()) {
            if (username.isNullOrBlank()) {
                throw IllegalArgumentException("credentials config invalid: log.elasticsearch.username")
            }
            if (password.isNullOrBlank()) {
                throw IllegalArgumentException("credentials config invalid: log.elasticsearch.password")
            }
            val provider = BasicCredentialsProvider()
            provider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))
            provider
        } else null

        // SSL证书配置
        if (hasCertificateConfig()) {
            if (keystoreFilePath.isNullOrBlank()) {
                throw IllegalArgumentException("SearchGuard config invalid: log.elasticsearch.keystore.filePath")
            }
            if (truststoreFilePath.isNullOrBlank()) {
                throw IllegalArgumentException("SearchGuard  config invalid: log.elasticsearch.keystore.password")
            }
            if (keystorePassword.isNullOrBlank()) {
                throw IllegalArgumentException("SearchGuard config invalid: log.elasticsearch.truststore.filePath")
            }
            if (truststorePassword.isNullOrBlank()) {
                throw IllegalArgumentException("SearchGuard config invalid: log.elasticsearch.truststore.password")
            }

            val keystoreFile = File(keystoreFilePath)
            if (!keystoreFile.exists()) {
                throw IllegalArgumentException("keystore file not found, please check: $keystoreFilePath")
            }
            val truststoreFile = File(truststoreFilePath)
            if (!truststoreFile.exists()) {
                throw IllegalArgumentException("truststore file not found, please check: $truststoreFilePath")
            }

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            val keystorePasswordCharArray = keystorePassword.toCharArray()
            keyStore.load(FileInputStream(keystoreFile), keystorePasswordCharArray)
            val truststore = KeyStore.getInstance(KeyStore.getDefaultType())
            val truststorePasswordCharArray = truststorePassword.toCharArray()
            truststore.load(FileInputStream(truststoreFile), truststorePasswordCharArray)

            sslContext = SSLContexts.custom()
                .loadTrustMaterial(truststore, null)
                .loadKeyMaterial(keyStore, keystorePasswordCharArray)
                .build()
        }

        client = RestHighLevelClient(
            ESConfigUtils.getClientBuilder(
                host = host,
                port = httpPort,
                https = boolConvert(https),
                tcpKeepAliveSeconds = tcpKeepAliveSeconds.toLong(),
                connectTimeout = connectTimeOut,
                socketTimeout = socketTimeout,
                connectionRequestTimeout = connectionRequestTimeOut,
                maxConnectNum = maxConnectNum,
                maxConnectPerRoute = maxConnectPerRoute,
                sslContext = sslContext,
                credentialsProvider = credentialsProvider
            )
        )
        return ESClient(
            clusterName = name,
            restClient = client!!,
            shards = indexShards,
            replicas = indexReplicas,
            shardsPerNode = indexShardsPerNode,
            requestTimeout = requestTimeout.toLong()
        )
    }

    @Bean
    fun esLogService(
        @Autowired logESClient: LogClient,
        @Autowired indexService: IndexService,
        @Autowired logStatusService: LogStatusService,
        @Autowired logTagService: LogTagService,
        @Autowired defaultKeywords: List<String>,
        @Autowired createIndexBean: CreateIndexBean,
        @Autowired logStorageBean: LogStorageBean,
        @Autowired redisOperation: RedisOperation,
        @Autowired buildLogPrintService: BuildLogPrintService
    ): LogService {
        return LogServiceESImpl(
            logClient = logESClient,
            indexService = indexService,
            logStatusService = logStatusService,
            logTagService = logTagService,
            logStorageBean = logStorageBean,
            createIndexBean = createIndexBean,
            buildLogPrintService = buildLogPrintService,
            redisOperation = redisOperation
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun logClient(@Autowired transportClient: ESClient): LogClient =
        LogClientImpl(transportClient)

    override fun destroy() {
        client?.close()
    }

    private fun boolConvert(value: String?): Boolean {
        return if (!value.isNullOrBlank()) {
            value.toBoolean()
        } else {
            false
        }
    }

    private fun hasCertificateConfig(): Boolean {
        return !keystoreFilePath.isNullOrBlank() ||
            !truststoreFilePath.isNullOrBlank() ||
            !keystorePassword.isNullOrBlank() ||
            !truststorePassword.isNullOrBlank()
    }
}

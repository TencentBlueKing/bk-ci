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

package com.tencent.devops.log.configuration

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.WebAutoConfiguration
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.log.LogMessageCode.ES_CLUSTER_ADDRESS_NOT_CONFIGURED
import com.tencent.devops.log.LogMessageCode.ES_CLUSTER_NAME_NOT_CONFIGURED
import com.tencent.devops.log.LogMessageCode.ES_UNIQUE_NAME_NOT_CONFIGURED
import com.tencent.devops.log.client.impl.MultiESLogClient
import com.tencent.devops.log.dao.IndexDao
import com.tencent.devops.log.dao.TencentIndexDao
import com.tencent.devops.log.es.ESAutoConfiguration
import com.tencent.devops.log.es.ESClient
import com.tencent.devops.log.es.ESProperties
import com.tencent.devops.log.util.ESConfigUtils
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.client.RestHighLevelClient
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Configuration
@ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "elasticsearch")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@AutoConfigureBefore(WebAutoConfiguration::class)
@AutoConfigureAfter(ESAutoConfiguration::class)
@EnableConfigurationProperties(ESProperties::class)
class LogESAutoConfiguration {
    @Value("\${log.elasticsearch.ip:#{null}}")
    private val e1IP: String? = null
    @Value("\${log.elasticsearch.port:#{null}}")
    private val e1Port: Int? = null
    @Value("\${log.elasticsearch.cluster:#{null}}")
    private val e1Cluster: String? = null
    @Value("\${log.elasticsearch.username:#{null}}")
    private val e1Username: String? = null
    @Value("\${log.elasticsearch.password:#{null}}")
    private val e1Password: String? = null
    @Value("\${log.elasticsearch.name:#{null}}")
    private val e1Name: String? = null
    @Value("\${log.elasticsearch.mainCluster:#{null}}")
    private val e1MainCluster: String? = null
    @Value("\${log.elasticsearch.writable:#{null}}")
    private val e1Writable: String? = null
    @Value("\${log.elasticsearch.shards:#{null}}")
    private val e1Shards: Int? = null
    @Value("\${log.elasticsearch.replicas:#{null}}")
    private val e1Replicas: Int? = null
    @Value("\${log.elasticsearch.shardsPerNode:#{null}}")
    private val e1ShardsPerNode: Int? = null
    @Value("\${log.elasticsearch.socketTimeout:#{null}}")
    private val e1socketTimeout: Int? = null
    @Value("\${log.elasticsearch.https:#{null}}")
    private val e1Https: String? = null

    @Value("\${log.elasticsearch2.ip:#{null}}")
    private val e2IP: String? = null
    @Value("\${log.elasticsearch2.port:#{null}}")
    private val e2Port: Int? = null
    @Value("\${log.elasticsearch2.cluster:#{null}}")
    private val e2Cluster: String? = null
    @Value("\${log.elasticsearch2.username:#{null}}")
    private val e2Username: String? = null
    @Value("\${log.elasticsearch2.password:#{null}}")
    private val e2Password: String? = null
    @Value("\${log.elasticsearch2.name:#{null}}")
    private val e2Name: String? = null
    @Value("\${log.elasticsearch2.mainCluster:#{null}}")
    private val e2MainCluster: String? = null
    @Value("\${log.elasticsearch2.writable:#{null}}")
    private val e2Writable: String? = null
    @Value("\${log.elasticsearch2.shards:#{null}}")
    private val e2Shards: Int? = null
    @Value("\${log.elasticsearch2.replicas:#{null}}")
    private val e2Replicas: Int? = null
    @Value("\${log.elasticsearch2.shardsPerNode:#{null}}")
    private val e2ShardsPerNode: Int? = null
    @Value("\${log.elasticsearch2.socketTimeout:#{null}}")
    private val e2socketTimeout: Int? = null
    @Value("\${log.elasticsearch.https:#{null}}")
    private val e2Https: String? = null

    private val tcpKeepAliveSeconds = 30000 // 探活连接时长
    private val connectTimeout = 1000 // 请求连接超时
    private val connectionRequestTimeout = 500 // 获取连接的超时时间
    private val maxConnectNum = 1000 // 最大连接数
    private val maxConnectPerRoute = 300 // 最大单节点连接数

    fun client(): ESClient {
        if (e1IP.isNullOrBlank()) {
            throw IllegalArgumentException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ES_CLUSTER_ADDRESS_NOT_CONFIGURED
                )
            )
        }
        if (e1Cluster.isNullOrBlank()) {
            throw IllegalArgumentException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ES_CLUSTER_NAME_NOT_CONFIGURED
                )
            )
        }
        if (e1Name.isNullOrBlank()) {
            throw IllegalArgumentException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ES_UNIQUE_NAME_NOT_CONFIGURED
                )
            )
        }

        val httpPort = e1Port ?: 9200
        val indexShards = e1Shards ?: 1 // 索引总分片数
        val indexReplicas = e1Replicas ?: 1 // 分片副本数
        val indexShardsPerNode = e1ShardsPerNode ?: 1 // 每个节点分片数
        val socketTimeout = e1socketTimeout ?: 5000 // 等待连接响应超时
        val requestTimeout = if (socketTimeout > 0) { // ES响应超时，取主动超时的一半
            socketTimeout / 2
        } else {
            30000
        }

        val credentialsProvider = getBasicCredentialsProvider(e1Username!!, e1Password!!)
        val builder = ESConfigUtils.getClientBuilder(
            host = e1IP!!,
            port = httpPort,
            https = boolConvert(e1Https),
            tcpKeepAliveSeconds = tcpKeepAliveSeconds.toLong(),
            connectTimeout = connectTimeout,
            socketTimeout = socketTimeout,
            connectionRequestTimeout = connectionRequestTimeout,
            maxConnectNum = maxConnectNum,
            maxConnectPerRoute = maxConnectPerRoute,
            sslContext = null,
            credentialsProvider = credentialsProvider
        )
        logger.info("Init the log es1 transport client with host($e1Name:$e1MainCluster:$e1IP:$e1Port), cluster($e1Cluster), credential($e1Username|$e1Password)")
        return ESClient(
            clusterName = e1Name!!,
            restClient = RestHighLevelClient(builder),
            shards = indexShards,
            replicas = indexReplicas,
            shardsPerNode = indexShardsPerNode,
            requestTimeout = requestTimeout.toLong(),
            mainCluster = boolConvert(e1MainCluster),
            writable = boolConvert(e1Writable)
        )
    }

    fun client2(): ESClient {
        if (e2IP.isNullOrBlank()) {
            throw IllegalArgumentException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ES_CLUSTER_ADDRESS_NOT_CONFIGURED,
                    params = arrayOf("2")
                )
            )
        }
        if (e2Cluster.isNullOrBlank()) {
            throw IllegalArgumentException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ES_CLUSTER_NAME_NOT_CONFIGURED,
                    params = arrayOf("2")
                )
            )
        }
        if (e2Name.isNullOrBlank()) {
            throw IllegalArgumentException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ES_UNIQUE_NAME_NOT_CONFIGURED,
                    params = arrayOf("2")
                )
            )
        }

        val httpPort = e2Port ?: 9200
        val indexShards = e2Shards ?: 1 // 索引总分片数
        val indexReplicas = e2Replicas ?: 1 // 分片副本数
        val indexShardsPerNode = e2ShardsPerNode ?: 1 // 每个节点分片数
        val socketTimeout = e2socketTimeout ?: 5000 // 等待连接响应超时
        val requestTimeout = if (socketTimeout > 0) { // ES响应超时，取主动超时的一半
            socketTimeout / 2
        } else {
            30000
        }

        val credentialsProvider = getBasicCredentialsProvider(e2Username!!, e2Password!!)
        val builder = ESConfigUtils.getClientBuilder(
            host = e2IP!!,
            port = httpPort,
            https = boolConvert(e2Https),
            tcpKeepAliveSeconds = tcpKeepAliveSeconds.toLong(),
            connectTimeout = connectTimeout,
            socketTimeout = socketTimeout,
            connectionRequestTimeout = connectionRequestTimeout,
            maxConnectNum = maxConnectNum,
            maxConnectPerRoute = maxConnectPerRoute,
            sslContext = null,
            credentialsProvider = credentialsProvider
        )
        logger.info("Init the log es2 transport client with host($e2Name:$e2MainCluster:$e2IP:$e2Port), cluster($e2Cluster), credential($e2Username|$e2Password)")
        return ESClient(
            clusterName = e2Name!!,
            restClient = RestHighLevelClient(builder),
            shards = indexShards,
            replicas = indexReplicas,
            shardsPerNode = indexShardsPerNode,
            requestTimeout = requestTimeout.toLong(),
            mainCluster = boolConvert(e2MainCluster),
            writable = boolConvert(e2Writable)
        )
    }

    @Bean
    @Primary
    fun logClient(
        @Autowired redisOperation: RedisOperation,
        @Autowired tencentIndexDao: TencentIndexDao,
        @Autowired indexDao: IndexDao,
        @Autowired dslContext: DSLContext
    ) = MultiESLogClient(listOf(client(), client2()), redisOperation, dslContext, tencentIndexDao, indexDao)

    private fun getBasicCredentialsProvider(username: String, password: String): BasicCredentialsProvider {
        val provider = BasicCredentialsProvider()
        provider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))
        return provider
    }

    private fun boolConvert(value: String?): Boolean {
        return if (!value.isNullOrBlank()) {
            value!!.toBoolean()
        } else {
            false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LogESAutoConfiguration::class.java)
    }
}

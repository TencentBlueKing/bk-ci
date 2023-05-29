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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.AgentTelegrafData
import com.tencent.devops.environment.pojo.TelegrafMulData
import com.tencent.devops.environment.pojo.TelegrafStandData
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TencentAgentMetricService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val objectMapper: ObjectMapper,
    private val kafkaClient: KafkaClient,
) : AgentMetricService(
    dslContext, thirdPartyAgentDao
) {

    @Value("\${kafka.topics.agentMetricTopic:#{null}}")
    val agentMetricTopic: String? = null

    override fun reportAgentMetrics(data: String): Boolean {
        logger.debug("reportAgentMetrics|origin")
        logger.debug(data)

        val startTime = System.currentTimeMillis()

        // 装换json类型，不是列表格式就是单独格式
        val jsonData = try {
            objectMapper.readValue<TelegrafMulData>(data)
        } catch (e: Exception) {
            logger.warn("reportAgentMetrics parse mul json error $data", e)
            objectMapper.readValue<TelegrafStandData>(data)
        }

        // 拼接成数据平台类型
        val reportData: List<AgentTelegrafData>? = when (jsonData) {
            is TelegrafMulData -> {
                jsonData.metrics?.map { dd ->
                    AgentTelegrafData(
                        dimensions = dd.tags?.map { it.key to it.value.toString() }?.toMap(),
                        time = dd.timestamp,
                        metrics = dd.fields?.map { "${dd.name ?: ""}_${it.key}" to it.value }?.toMap()
                    )
                }
            }

            is TelegrafStandData -> {
                listOf(
                    AgentTelegrafData(
                        dimensions = jsonData.tags?.map { it.key to it.value.toString() }?.toMap(),
                        time = jsonData.timestamp,
                        metrics = jsonData.fields?.map { "${jsonData.name ?: ""}_${it.key}" to it.value }?.toMap()
                    )
                )
            }

            else -> {
                emptyList()
            }
        }

        logger.info("reportAgentMetrics json cost ${System.currentTimeMillis() - startTime}")

        // 上报
        if (!agentMetricTopic.isNullOrBlank()) {
            logger.debug("reportAgentMetrics|kafka")
            reportData?.forEach {
                val d = objectMapper.writeValueAsString(it)
                logger.debug(d)
                kafkaClient.send(agentMetricTopic!!, d)
            }
        } else {
            logger.error("agentMetricTopic is null")
        }

        logger.info("reportAgentMetrics report cost ${System.currentTimeMillis() - startTime}")

        return true
    }

    override fun queryCpuUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        return super.queryCpuUsageMetrix(userId, projectId, nodeHashId, timeRange)
    }

    override fun queryMemoryUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        return super.queryMemoryUsageMetrix(userId, projectId, nodeHashId, timeRange)
    }

    override fun queryDiskioMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        return super.queryDiskioMetrix(userId, projectId, nodeHashId, timeRange)
    }

    override fun queryNetMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        return super.queryNetMetrix(userId, projectId, nodeHashId, timeRange)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentMetricService::class.java)
    }
}

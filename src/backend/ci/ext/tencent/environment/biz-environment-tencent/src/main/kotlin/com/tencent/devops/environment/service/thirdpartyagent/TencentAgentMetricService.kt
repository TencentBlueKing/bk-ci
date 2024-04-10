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

package com.tencent.devops.environment.service.thirdpartyagent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.environment.client.InfluxdbClient
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentHostInfo
import com.tencent.devops.environment.pojo.AgentTelegrafData
import com.tencent.devops.environment.pojo.TelegrafMulData
import com.tencent.devops.environment.pojo.TelegrafStandData
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.AgentPropsScope
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Primary
@Service
class TencentAgentMetricService @Autowired constructor(
    val dslContext: DSLContext,
    val thirdPartyAgentDao: ThirdPartyAgentDao,
    val influxdbClient: InfluxdbClient,
    private val objectMapper: ObjectMapper,
    private val kafkaClient: KafkaClient,
    private val bkMonitorMetricsService: BkMonitorMetricsService,
    private val agentPropsScope: AgentPropsScope
) : AgentMetricService(
    dslContext,
    thirdPartyAgentDao,
    influxdbClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AgentMetricService::class.java)

        private const val AGENT_TELEGRAF_CPU_DETAIL = "cpu_detail"
        private const val AGENT_TELEGRAF_NET = "net"
        private const val AGENT_TELEGRAF_MEM = "mem"
        private const val AGENT_TELEGRAF_IO = "io"
        private const val AGENT_TELEGRAF_DISK = "disk"
        private const val AGENT_TELEGRAF_SWAP = "swap"
        private const val AGENT_TELEGRAF_LOAD = "load"
        private const val AGENT_TELEGRAF_NETSTAT = "netstat"
        private const val AGENT_TELEGRAF_ENV = "env"
    }

    @Value("\${kafka.topics.agentMetricCpuDetailTopic:#{null}}")
    val agentMetricCpuDetailTopic: String? = null

    @Value("\${kafka.topics.agentMetricNetTopic:#{null}}")
    val agentMetricNetTopic: String? = null

    @Value("\${kafka.topics.agentMetricMemTopic:#{null}}")
    val agentMetricMemTopic: String? = null

    @Value("\${kafka.topics.agentMetricIoTopic:#{null}}")
    val agentMetricIoTopic: String? = null

    @Value("\${kafka.topics.agentMetricDiskTopic:#{null}}")
    val agentMetricDiskTopic: String? = null

    @Value("\${kafka.topics.agentMetricSwapTopic:#{null}}")
    val agentMetricSwapTopic: String? = null

    @Value("\${kafka.topics.agentMetricLoadTopic:#{null}}")
    val agentMetricLoadTopic: String? = null

    @Value("\${kafka.topics.agentMetricNetstatTopic:#{null}}")
    val agentMetricNetstatTopic: String? = null

    @Value("\${kafka.topics.agentMetricEnvTopic:#{null}}")
    val agentMetricEnvTopic: String? = null

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

        when (jsonData) {
            is TelegrafMulData -> {
                // 将相同表单的聚合在一起
                val tableMap = mutableMapOf<String, MutableList<AgentTelegrafData>>()
                tableMap[AGENT_TELEGRAF_CPU_DETAIL] = mutableListOf()
                tableMap[AGENT_TELEGRAF_NET] = mutableListOf()
                tableMap[AGENT_TELEGRAF_MEM] = mutableListOf()
                tableMap[AGENT_TELEGRAF_IO] = mutableListOf()
                tableMap[AGENT_TELEGRAF_DISK] = mutableListOf()
                tableMap[AGENT_TELEGRAF_SWAP] = mutableListOf()
                tableMap[AGENT_TELEGRAF_LOAD] = mutableListOf()
                tableMap[AGENT_TELEGRAF_NETSTAT] = mutableListOf()
                tableMap[AGENT_TELEGRAF_ENV] = mutableListOf()

                jsonData.metrics?.forEach { metric ->
                    val new = AgentTelegrafData(
                        dimensions = metric.tags?.map { t -> t.key to t.value.toString() }?.toMap(),
                        time = metric.timestamp,
                        metrics = metric.fields
                    )

                    tableMap[metric.name]?.add(new)
                }

                tableMap.forEach { (table, list) ->
                    sendMetric(table, list)
                }
            }

            is TelegrafStandData -> {
                sendMetric(
                    jsonData.name,
                    listOf(
                        AgentTelegrafData(
                            dimensions = jsonData.tags?.map { t -> t.key to t.value.toString() }?.toMap(),
                            time = jsonData.timestamp,
                            metrics = jsonData.fields
                        )
                    )
                )
            }
        }

        logger.info("reportAgentMetrics report cost ${System.currentTimeMillis() - startTime}")

        return true
    }

    private fun sendMetric(tableName: String?, data: List<AgentTelegrafData>) {
        if (data.isEmpty()) {
            return
        }

        val topicName = when (tableName) {
            AGENT_TELEGRAF_CPU_DETAIL -> agentMetricCpuDetailTopic
            AGENT_TELEGRAF_NET -> agentMetricNetTopic
            AGENT_TELEGRAF_MEM -> agentMetricMemTopic
            AGENT_TELEGRAF_IO -> agentMetricIoTopic
            AGENT_TELEGRAF_DISK -> agentMetricDiskTopic
            AGENT_TELEGRAF_SWAP -> agentMetricSwapTopic
            AGENT_TELEGRAF_LOAD -> agentMetricLoadTopic
            AGENT_TELEGRAF_NETSTAT -> agentMetricNetstatTopic
            AGENT_TELEGRAF_ENV -> agentMetricEnvTopic
            else -> return
        }

        if (topicName.isNullOrBlank()) {
            logger.error("$tableName's agentMetricTopic is null")
            return
        }

        kafkaClient.send(topicName, objectMapper.writeValueAsString(data))
    }

    override fun queryCpuUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val agentRecord = getAgentRecord(
            nodeHashId = nodeHashId,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        if (!checkAgentProject(projectId)) {
            return super.queryCpuUsageMetrix(userId, projectId, nodeHashId, timeRange)
        }
        if (!checkAgentVersion(agentRecord.masterVersion)) {
            return super.queryCpuUsageMetrix(userId, projectId, nodeHashId, timeRange)
        }

        return bkMonitorMetricsService.queryCpuUsageMetrics(
            userId = userId,
            projectId = projectId,
            agentHashId = HashUtil.encodeLongId(agentRecord.id),
            timeRange = timeRange
        )
    }

    override fun queryMemoryUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val agentRecord = getAgentRecord(
            nodeHashId = nodeHashId,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        if (!checkAgentProject(projectId)) {
            return super.queryMemoryUsageMetrix(userId, projectId, nodeHashId, timeRange)
        }
        if (!checkAgentVersion(agentRecord.masterVersion)) {
            return super.queryMemoryUsageMetrix(userId, projectId, nodeHashId, timeRange)
        }

        return bkMonitorMetricsService.queryMemoryUsageMetrics(
            userId = userId,
            projectId = projectId,
            agentHashId = HashUtil.encodeLongId(agentRecord.id),
            timeRange = timeRange
        )
    }

    override fun queryDiskioMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val agentRecord = getAgentRecord(
            nodeHashId = nodeHashId,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        if (!checkAgentProject(projectId)) {
            return super.queryDiskioMetrix(userId, projectId, nodeHashId, timeRange)
        }
        if (!checkAgentVersion(agentRecord.masterVersion)) {
            return super.queryDiskioMetrix(userId, projectId, nodeHashId, timeRange)
        }

        return bkMonitorMetricsService.queryDiskioMetrics(
            userId = userId,
            projectId = projectId,
            agentHashId = HashUtil.encodeLongId(agentRecord.id),
            os = agentRecord.os,
            timeRange = timeRange
        )
    }

    override fun queryNetMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val agentRecord = getAgentRecord(
            nodeHashId = nodeHashId,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        if (!checkAgentProject(projectId)) {
            return super.queryNetMetrix(userId, projectId, nodeHashId, timeRange)
        }
        if (!checkAgentVersion(agentRecord.masterVersion)) {
            return super.queryNetMetrix(userId, projectId, nodeHashId, timeRange)
        }

        return bkMonitorMetricsService.queryNetMetrics(
            userId = userId,
            projectId = projectId,
            agentHashId = HashUtil.encodeLongId(agentRecord.id),
            os = agentRecord.os,
            timeRange = timeRange
        )
    }

    override fun queryHostInfo(projectId: String, agentHashId: String): AgentHostInfo {
        if (!checkAgentProject(projectId)) {
            return super.queryHostInfo(projectId, agentHashId)
        }
        return bkMonitorMetricsService.queryHostInfo("$projectId:$agentHashId")
    }

    private fun getAgentRecord(
        nodeHashId: String,
        projectId: String
    ): TEnvironmentThirdpartyAgentRecord? {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        return thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        )
    }

    // agent升级是一批一批的，所以根据agent版本决定使用新的还是旧的环境查询监控数据
    private fun checkAgentVersion(agentVersion: String): Boolean {
        return agentVersion == agentPropsScope.getAgentVersion()
    }

    // 区分 stream 项目使用模板分割，PAC 上线后删除
    private fun checkAgentProject(projectId: String): Boolean {
        return !projectId.startsWith("git_")
    }
}

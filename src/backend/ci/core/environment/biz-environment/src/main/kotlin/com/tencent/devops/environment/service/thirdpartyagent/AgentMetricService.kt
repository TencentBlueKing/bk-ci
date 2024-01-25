package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.client.UsageMetrics
import com.tencent.devops.environment.client.InfluxdbClient
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentHostInfo
import javax.ws.rs.NotFoundException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AgentMetricService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val influxdbClient: InfluxdbClient
) {

    fun reportAgentMetrics(
        data: String
    ): Boolean {
        // 开源版本直接上报给influxdb，不需要这个接口
        return true
    }

    fun queryCpuUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        return try {
            UsageMetrics.loadMetricsBean(UsageMetrics.MetricsType.CPU, OS.valueOf(agentRecord.os))
                ?.loadQuery(
                    agentHashId = HashUtil.encodeLongId(agentRecord.id),
                    timeRange = timeRange
                ) ?: emptyMap()
        } catch (e: Throwable) {
            logger.warn("influx query error: ", e)
            emptyMap()
        }
    }

    fun queryMemoryUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")
        return try {
            UsageMetrics.loadMetricsBean(UsageMetrics.MetricsType.MEMORY, OS.valueOf(agentRecord.os))
                ?.loadQuery(
                    agentHashId = HashUtil.encodeLongId(agentRecord.id),
                    timeRange = timeRange
                ) ?: emptyMap()
        } catch (e: Throwable) {
            logger.warn("influx query error: ", e)
            emptyMap()
        }
    }

    fun queryDiskioMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        return try {
            UsageMetrics.loadMetricsBean(UsageMetrics.MetricsType.DISK, OS.valueOf(agentRecord.os))
                ?.loadQuery(
                    agentHashId = HashUtil.encodeLongId(agentRecord.id),
                    timeRange = timeRange
                ) ?: emptyMap()
        } catch (e: Throwable) {
            logger.warn("influx query error: ", e)
            emptyMap()
        }
    }

    fun queryNetMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        return try {
            UsageMetrics.loadMetricsBean(UsageMetrics.MetricsType.NET, OS.valueOf(agentRecord.os))
                ?.loadQuery(
                    agentHashId = HashUtil.encodeLongId(agentRecord.id),
                    timeRange = timeRange
                ) ?: emptyMap()
        } catch (e: Throwable) {
            logger.warn("influx query error: ", e)
            emptyMap()
        }
    }

    fun queryHostInfo(projectId: String, agentHashId: String): AgentHostInfo {
        return influxdbClient.queryHostInfo(agentHashId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentMetricService::class.java)
    }
}

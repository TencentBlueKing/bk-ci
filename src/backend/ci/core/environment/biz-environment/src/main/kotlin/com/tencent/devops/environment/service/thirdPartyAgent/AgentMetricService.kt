package com.tencent.devops.environment.service.thirdPartyAgent

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.client.UsageMetrics
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import javax.ws.rs.NotFoundException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AgentMetricService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao
) {

    fun reportAgentMetrics(
        data: String
    ): Boolean {
        // 开源版本需自主实现
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

    companion object {
        private val logger = LoggerFactory.getLogger(AgentMetricService::class.java)
    }
}

package com.tencent.devops.environment.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.environment.agent.client.EsbAgentClient
import com.tencent.devops.common.environment.agent.pojo.agent.CmdbServerPage
import com.tencent.devops.common.environment.agent.pojo.agent.RawCcNode
import com.tencent.devops.common.environment.agent.pojo.agent.RawCmdbNode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import org.jooq.DSLContext

object ImportServerNodeUtils {
    fun getUserCmdbNode(
        esbAgentClient: EsbAgentClient,
        redisOperation: RedisOperation,
        userId: String,
        offset: Int,
        limit: Int
    ): List<RawCmdbNode> {
        val key = "env_node_buffer_cmdb_${userId}_${offset}_$limit"
        val buffer = redisOperation.get(key)

        return if (buffer != null) {
            return jacksonObjectMapper().readValue(buffer)
        } else {
            val cmdbNodes = esbAgentClient.getUserCmdbNode(userId, offset, limit)
            redisOperation.set(key, jacksonObjectMapper().writeValueAsString(cmdbNodes), 60)
            cmdbNodes
        }
    }

    fun getUserCmdbNodeNew(
        esbAgentClient: EsbAgentClient,
        redisOperation: RedisOperation,
        userId: String,
        bakOperator: Boolean,
        ips: List<String>,
        offset: Int,
        limit: Int
    ): CmdbServerPage {
        // 对没有IP条件的查询，做缓存
        if (ips.isEmpty()) {
            val key = "env_node_buffer_cmdb_${userId}_${offset}_${limit}_$bakOperator"
            val buffer = redisOperation.get(key)
            return if (buffer != null) {
                jacksonObjectMapper().readValue(buffer)
            } else {
                val cmdbNodePage = esbAgentClient.getUserCmdbNodeNew(userId, bakOperator, ips, offset, limit)
                redisOperation.set(key, jacksonObjectMapper().writeValueAsString(cmdbNodePage), 60)
                cmdbNodePage
            }
        }

        return esbAgentClient.getUserCmdbNodeNew(userId, bakOperator, ips, offset, limit)
    }

    fun getUserCcNode(esbAgentClient: EsbAgentClient, redisOperation: RedisOperation, userId: String): List<RawCcNode> {
        val key = "env_node_buffer_cc_$userId"
        val buffer = redisOperation.get(key)
        return if (buffer != null) {
            jacksonObjectMapper().readValue(buffer)
        } else {
            val ccNodes = esbAgentClient.getUserCCNodes(userId)
            redisOperation.set(key, jacksonObjectMapper().writeValueAsString(ccNodes), 60)
            ccNodes
        }
    }

    fun checkImportCount(
        esbAgentClient: EsbAgentClient,
        dslContext: DSLContext,
        projectConfigDao: ProjectConfigDao,
        nodeDao: NodeDao,
        projectId: String,
        userId: String,
        toAddNodeCount: Int
    ) {
        val projectConfig = projectConfigDao.get(dslContext, projectId, userId)
        val importQuata = projectConfig.importQuota
        val existImportNodeCount = nodeDao.countImportNode(dslContext, projectId)
        if (toAddNodeCount + existImportNodeCount > importQuata) {
            throw OperationException("导入CC/CMDB节点数不能超过配额[$importQuata]，如有特别需求，请联系【蓝盾助手】")
        }
    }
}
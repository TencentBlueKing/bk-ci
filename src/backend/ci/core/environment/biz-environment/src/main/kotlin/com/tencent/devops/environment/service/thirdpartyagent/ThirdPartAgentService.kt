package com.tencent.devops.environment.service.thirdpartyagent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.AgentAction
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentActionDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartAgentUpdateType
import com.tencent.devops.environment.utils.ThirdAgentActionAddLock
import com.tencent.devops.environment.utils.ThirdAgentUpdateEnvLock
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 对第三方构建机一些自身数据操作
 */
@Suppress("NestedBlockDepth")
@Service
class ThirdPartAgentService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val agentActionDao: ThirdPartyAgentActionDao,
    private val agentDao: ThirdPartyAgentDao
) {
    fun addAgentAction(
        projectId: String,
        agentId: Long,
        action: AgentAction
    ) {
        val lock = ThirdAgentActionAddLock(redisOperation, projectId, agentId)
        if (agentActionDao.getAgentLastAction(dslContext, projectId, agentId) == action.name) {
            return
        }
        try {
            lock.lock()
            if (agentActionDao.getAgentLastAction(dslContext, projectId, agentId) == action.name) {
                return
            }
            agentActionDao.addAgentAction(
                dslContext = dslContext,
                projectId = projectId,
                agentId = agentId,
                action = action.name
            )
        } finally {
            lock.unlock()
        }
    }

    fun fetchAgentEnv(
        userId: String,
        projectId: String,
        nodeHashIds: Set<String>?,
        agentHashIds: Set<String>?
    ): Map<String, List<EnvVar>> {
        if (agentHashIds.isNullOrEmpty() && nodeHashIds.isNullOrEmpty()) {
            return emptyMap()
        }

        val res = mutableMapOf<String, List<EnvVar>>()
        if (!agentHashIds.isNullOrEmpty()) {
            val idMap = agentHashIds.associateBy { HashUtil.decodeIdToLong(it) }
            val agents = agentDao.getAgentByAgentIds(dslContext, idMap.keys, projectId)
            agents.forEach { agent ->
                res[idMap[agent.id] ?: return@forEach] = if (agent.agentEnvs.isNullOrBlank()) {
                    emptyList()
                } else {
                    objectMapper.readValue<List<EnvVar>>(agent.agentEnvs)
                }
            }
        } else {
            val idMap = nodeHashIds!!.associateBy { HashUtil.decodeIdToLong(it) }
            val agents = agentDao.getAgentsByNodeIds(dslContext, idMap.keys, projectId)
            agents.forEach { agent ->
                res[idMap[agent.nodeId] ?: return@forEach] = if (agent.agentEnvs.isNullOrBlank()) {
                    emptyList()
                } else {
                    objectMapper.readValue<List<EnvVar>>(agent.agentEnvs)
                }
            }
        }
        return res
    }

    fun batchUpdateAgentEnv(
        userId: String,
        projectId: String,
        nodeHashIds: Set<String>?,
        agentHashIds: Set<String>?,
        type: ThirdPartAgentUpdateType?,
        data: List<EnvVar>
    ): Boolean {
        if (agentHashIds.isNullOrEmpty() && nodeHashIds.isNullOrEmpty()) {
            return false
        }

        val agents = if (!agentHashIds.isNullOrEmpty()) {
            agentDao.getAgentByAgentIds(dslContext, agentHashIds.map { HashUtil.decodeIdToLong(it) }.toSet(), projectId)
        } else {
            agentDao.getAgentsByNodeIds(
                dslContext,
                nodeHashIds!!.map { HashUtil.decodeIdToLong(it) }.toSet(),
                projectId
            )
        }.ifEmpty { return false }

        when (type ?: ThirdPartAgentUpdateType.UPDATE) {
            ThirdPartAgentUpdateType.ADD, ThirdPartAgentUpdateType.REMOVE -> {
                agents.forEach { agent ->
                    val agentId = agent.id
                    val lock = ThirdAgentUpdateEnvLock(redisOperation, projectId, agentId)
                    try {
                        lock.lock()
                        val oldEnvsMap = if (agent.agentEnvs.isNullOrBlank()) {
                            mutableMapOf()
                        } else {
                            objectMapper.readValue<List<EnvVar>>(agent.agentEnvs).associateBy { it.name }.toMutableMap()
                        }
                        val newEnvsMap = data.associateBy { it.name }.toMutableMap()
                        val envs = if (type == ThirdPartAgentUpdateType.ADD) {
                            oldEnvsMap.putAll(newEnvsMap)
                            oldEnvsMap.values
                        } else {
                            newEnvsMap.keys.forEach { key ->
                                oldEnvsMap.remove(key)
                            }
                            oldEnvsMap.values
                        }
                        agentDao.saveAgentEnvs(
                            dslContext = dslContext,
                            agentIds = setOf(agentId),
                            envStr = objectMapper.writeValueAsString(envs)
                        )
                    } finally {
                        lock.unlock()
                    }
                }
            }

            ThirdPartAgentUpdateType.UPDATE -> {
                agentDao.saveAgentEnvs(
                    dslContext = dslContext,
                    agentIds = agents.map { it.id }.toSet(),
                    envStr = objectMapper.writeValueAsString(data)
                )
                return true
            }

            else -> return false
        }
        return true
    }
}

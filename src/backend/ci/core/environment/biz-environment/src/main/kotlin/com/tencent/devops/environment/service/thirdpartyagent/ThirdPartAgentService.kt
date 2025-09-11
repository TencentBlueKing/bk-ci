package com.tencent.devops.environment.service.thirdpartyagent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.AgentAction
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentActionDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartAgentUpdateType
import com.tencent.devops.environment.pojo.thirdpartyagent.UpdateAgentInfo
import com.tencent.devops.environment.utils.ThirdAgentActionAddLock
import com.tencent.devops.environment.utils.ThirdAgentUpdateEnvLock
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
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
    private val agentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao,
    private val environmentPermissionService: EnvironmentPermissionService
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

        val nodeIds = agents.map { it.nodeId }.toSet()
        val permissionNodeIds =
            environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.EDIT)
        if (nodeIds.subtract(permissionNodeIds).isNotEmpty()) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }

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

    fun updateAgentInfo(
        userId: String,
        projectId: String,
        data: UpdateAgentInfo
    ): Boolean {
        if (data.nodeHashId.isNullOrBlank() && data.agentHashId.isNullOrBlank()) {
            return false
        }

        val nodeId = if (data.nodeHashId.isNullOrBlank()) {
            agentDao.getAgentByProject(dslContext, HashUtil.decodeIdToLong(data.agentHashId!!), projectId)?.nodeId
                ?: return false
        } else {
            HashUtil.decodeIdToLong(data.nodeHashId!!)
        }

        if (!environmentPermissionService.checkNodePermission(userId, projectId, nodeId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }

        dslContext.transaction { config ->
            if (!data.displayName.isNullOrBlank()) {
                nodeDao.updateDisplayName(
                    dslContext = dslContext,
                    nodeId = nodeId,
                    nodeName = data.displayName!!,
                    userId = userId,
                    projectId = projectId
                )
            }
            agentDao.updateAgentInfo(
                dslContext = dslContext,
                projectId = projectId,
                agentId = if (data.agentHashId.isNullOrBlank()) {
                    null
                } else {
                    HashUtil.decodeIdToLong(data.agentHashId!!)
                },
                nodeId = if (data.nodeHashId.isNullOrBlank()) {
                    null
                } else {
                    HashUtil.decodeIdToLong(data.nodeHashId!!)
                },
                parallelTaskCount = data.parallelTaskCount,
                dockerParallelTaskCount = data.dockerParallelTaskCount,
                envs = data.envs
            )
        }

        return true
    }

    fun batchSetParallelTaskCount(
        userId: String,
        projectId: String,
        nodeHashIds: List<String>,
        parallelTaskCount: Int?,
        dockerParallelTaskCount: Int?
    ) {
        if (parallelTaskCount == null && dockerParallelTaskCount == null) {
            return
        }
        val nodeIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) }.toSet()
        val permissionNodeIds =
            environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.EDIT)
        if (nodeIds.subtract(permissionNodeIds).isNotEmpty()) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val agentRecords = agentDao.getAgentsByNodeIds(
                dslContext = context,
                nodeIds = nodeIds,
                projectId = projectId
            )
            val recordNodeIds = agentRecords.map { it.nodeId }.toSet()
            val subIds = nodeIds.subtract(recordNodeIds)
            if (subIds.isNotEmpty()) {
                throw ErrorCodeException(
                    errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                    params = arrayOf(subIds.joinToString(separator = ","))
                )
            }
            agentDao.batchUpdateParallelTaskCount(
                dslContext = context,
                projectId = projectId,
                ids = agentRecords.map { it.id }.toSet(),
                parallelTaskCount = parallelTaskCount,
                dockerParallelTaskCount = dockerParallelTaskCount
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartAgentService::class.java)
    }
}

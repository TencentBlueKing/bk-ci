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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.exception.AgentPermissionUnAuthorizedException
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentHeartbeatInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentLink
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStatusWithInfo
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.FileMD5CacheUtils.getFileMD5
import com.tencent.devops.environment.utils.ThirdPartyAgentHeartbeatUtils
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class ThirdPartyAgentService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao,
    private val redisOperation: RedisOperation,
    private val envNodeDao: EnvNodeDao,
    private val envDao: EnvDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val downloadAgentInstallService: DownloadAgentInstallService
) {

    fun generateAgent(
        userId: String,
        projectId: String,
        os: OS,
        zoneName: String?
    ): ThirdPartyAgentLink {
        val gateway = slaveGatewayService.getGateway(zoneName)
        logger.info("Generate agent($os) info of project($projectId) with gateway $gateway by user($userId)")
        val unimportAgent = thirdPartyAgentDao.listUnimportAgent(dslContext, projectId, userId, os)
        val agent = if (unimportAgent.isEmpty()) {
            val secretKey = generateSecretKey()
            val id = thirdPartyAgentDao.add(dslContext, userId, projectId, os, SecurityUtil.encrypt(secretKey), gateway)
            val hashId = HashUtil.encodeLongId(id)
            Pair(hashId, secretKey)
        } else {
            val i = unimportAgent[0]
            logger.info("The agent(${i.id}) exist")
            val hashId = HashUtil.encodeLongId(i.id)
            val secretKey = SecurityUtil.decrypt(i.secretKey)
            if (!gateway.isNullOrBlank()) {
                thirdPartyAgentDao.updateGateway(dslContext, i.id, gateway!!)
            }
            Pair(hashId, secretKey)
        }
        // TODO Support windows scripts
        if (os == OS.WINDOWS) {
            return ThirdPartyAgentLink(
                agent.first,
                "$gateway/ms/environment/api/external/thirdPartyAgent/${agent.first}/agent"
            )
        }
        return ThirdPartyAgentLink(
            agent.first,
            "curl -H \"$AUTH_HEADER_DEVOPS_PROJECT_ID: $projectId\" $gateway/ms/environment/api/external/thirdPartyAgent/${agent.first}/install | bash"
        )
    }

    fun getAgentLink(
        userId: String,
        projectId: String,
        nodeId: String
    ): ThirdPartyAgentLink {
        val id = HashUtil.decodeIdToLong(nodeId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, id, projectId)
            ?: throw NotFoundException("The agent is not exist")
        val gw = slaveGatewayService.getGateway(agentRecord)
        val agentId = HashUtil.encodeLongId(agentRecord.id)
        return ThirdPartyAgentLink(
            agentId,
            "curl http://$gw/ms/environment/api/external/thirdPartyAgent/$agentId/install | bash"
        )
    }

    fun listAgents(
        userId: String,
        projectId: String,
        os: OS
    ): List<ThirdPartyAgentInfo> {
        val agents = thirdPartyAgentDao.listImportAgent(dslContext, projectId, os)
        if (agents.isEmpty()) {
            return arrayListOf()
        }

        val nodeIds = agents.filter { it.nodeId != null }.map { it.nodeId }
        val nodes = nodeDao.listByIds(dslContext, projectId, nodeIds)
        if (nodes.isEmpty()) {
            return emptyList()
        }
        val nodeMap = nodes.map { it.nodeId to it }.toMap()

        val agentInfo = ArrayList<ThirdPartyAgentInfo>()
        agents.forEach { agent ->
            if (agent.nodeId == null) {
                logger.warn("The agent(${agent.id}) node id is empty")
                return@forEach
            }

            val node = nodeMap[agent.nodeId]
            if (node == null || node.nodeStatus.isNullOrBlank()) {
                logger.warn("Fail to find the node status of agent(${agent.id})")
                return@forEach
            }

            agentInfo.add(
                ThirdPartyAgentInfo(
                    HashUtil.encodeLongId(agent.id),
                    projectId,
                    NodeStatus.valueOf(node.nodeStatus!!).statusName,
                    agent.hostname,
                    agent.ip,
                    node.displayName
                )
            )
        }
        return agentInfo
    }

    fun getAgentByDisplayName(projectId: String, displayName: String): AgentResult<ThirdPartyAgent?> {
        logger.info("[$projectId|$displayName] Get the agent")
        val nodes = nodeDao.getByDisplayName(dslContext, projectId, displayName, listOf(NodeType.THIRDPARTY.name))
        if (nodes.isEmpty()) {
            return AgentResult(0, null, null, null)
        }
        if (nodes.size != 1) {
            logger.warn("[$projectId|$displayName] There are more than one node with the display name - ($nodes)")
            return AgentResult(0, null, null, null)
        }
        val node = nodes[0]
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, node.nodeId, projectId)
        if (agentRecord == null) {
            logger.warn("[$projectId|$displayName|${node.nodeId}] Fail to get the agent")
            return AgentResult(0, null, null, null)
        }
        val status = AgentStatus.fromStatus(agentRecord.status)
        return AgentResult(
            status,
            ThirdPartyAgent(
                HashUtil.encodeLongId(agentRecord.id),
                projectId,
                HashUtil.encodeLongId(node.nodeId),
                status,
                agentRecord.hostname,
                agentRecord.os,
                agentRecord.ip,
                SecurityUtil.decrypt(agentRecord.secretKey),
                agentRecord.createdUser,
                agentRecord.createdTime.timestamp()
            )
        )
    }

    fun getAgent(
        projectId: String,
        agentId: String
    ): AgentResult<ThirdPartyAgent?> {
        logger.info("Get the agent($agentId) of project($projectId)")
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId)
            ?: return AgentResult(AgentStatus.DELETE, null)
        val status = AgentStatus.fromStatus(agentRecord.status)
        val nodeId = if (agentRecord.nodeId != null) {
            HashUtil.encodeLongId(agentRecord.nodeId)
        } else {
            null
        }
        return AgentResult(
            status, ThirdPartyAgent(
                agentId, projectId,
                nodeId,
                status,
                agentRecord.hostname,
                agentRecord.os,
                agentRecord.ip,
                SecurityUtil.decrypt(agentRecord.secretKey),
                agentRecord.createdUser,
                agentRecord.createdTime.timestamp()
            )
        )
    }

    fun getAgnetByEnvName(projectId: String, envName: String): List<ThirdPartyAgent> {
        logger.info("[$projectId|$envName] Get the agents by env name")
        val envRecord = envDao.getByEnvName(dslContext, projectId, envName)
        if (envRecord == null) {
            logger.warn("[$projectId|$envName] The env is not exist")
            return emptyList()
        }
        return getAgentByEnvId(projectId, HashUtil.encodeLongId(envRecord.envId))
    }

    fun getAgentByEnvId(projectId: String, envHashId: String): List<ThirdPartyAgent> {
        logger.info("[$projectId|$envHashId] Get the agents by envId")
        val envId = HashUtil.decodeIdToLong(envHashId)
        val nodes = envNodeDao.list(dslContext, projectId, listOf(envId))
        if (nodes.isEmpty()) {
            return emptyList()
        }
        val nodeIds = nodes.map {
            it.nodeId
        }.toSet()
        val agents = thirdPartyAgentDao.getAgentsByNodeIds(dslContext, nodeIds, projectId)
        if (agents.isEmpty()) {
            return emptyList()
        }
        return agents.map {
            val nodeId = if (it.nodeId != null) {
                HashUtil.encodeLongId(it.nodeId)
            } else {
                null
            }
            ThirdPartyAgent(
                HashUtil.encodeLongId(it.id),
                projectId,
                nodeId,
                AgentStatus.fromStatus(it.status),
                it.hostname,
                it.os,
                it.ip,
                SecurityUtil.decrypt(it.secretKey),
                it.createdUser,
                it.createdTime.timestamp()
            )
        }
    }

    fun checkIfCanUpgrade(
        projectId: String,
        agentId: String,
        secretKey: String,
        tag: String
    ): AgentResult<Boolean> {
        logger.info("Checking if the agent($agentId) of project($projectId) can upgrade")
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId)
            ?: return AgentResult(AgentStatus.DELETE, false)
        val status = AgentStatus.fromStatus(agentRecord.status)
        if (status != AgentStatus.IMPORT_OK) {
            return AgentResult(status, false)
        }

        val key = SecurityUtil.decrypt(agentRecord.secretKey)

        if (key != secretKey) {
            logger.warn("The agent($id) of project($projectId)'s secret($secretKey) is not match the expect one($key)")
            return AgentResult(AgentStatus.DELETE, false)
        }

        val jarFile = downloadAgentInstallService.getAgentJarFile()
        val md5 = getFileMD5(jarFile)
        if (md5 == tag) {
            return AgentResult(status, false)
        }
        logger.info("The agent($id) can upgrade")
        return AgentResult(status, true)
    }

    fun deleteAgent(
        userId: String,
        projectId: String,
        nodeId: String
    ) {
        logger.info("Delete the node($nodeId) of project($projectId) by user($userId)")
        val id = HashUtil.decodeIdToLong(nodeId)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val record = thirdPartyAgentDao.getAgentByNodeId(context, id, projectId)
            if (record == null) {
                logger.warn("The node($nodeId) is not exist")
                throw NotFoundException("The node is not exist")
            }
            val count = thirdPartyAgentDao.updateStatus(context, record.id, null, projectId, AgentStatus.DELETE)
            if (count != 1) {
                logger.warn("Can't delete the agent($count)")
            }

            nodeDao.updateNodeStatus(context, id, NodeStatus.DELETED)
        }
    }

    fun getAgentStatusWithInfo(
        userId: String,
        projectId: String,
        agentId: String
    ): ThirdPartyAgentStatusWithInfo {
        val record = thirdPartyAgentDao.getAgent(
            dslContext,
            HashUtil.decodeIdToLong(agentId), projectId
        ) ?: throw NotFoundException("The agent($agentId) is not exist")
        return ThirdPartyAgentStatusWithInfo(
            AgentStatus.fromStatus(record.status),
            record.hostname ?: "",
            record.ip ?: "",
            record.detectOs ?: ""
        )
    }

    fun importAgent(
        userId: String,
        projectId: String,
        agentId: String
    ) {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId)
            ?: throw NotFoundException("The agent($agentId) is not exist")
        if (agentRecord.status == AgentStatus.IMPORT_EXCEPTION.status ||
            agentRecord.status == AgentStatus.UN_IMPORT.status
        ) {
            logger.warn("The agent status(${agentRecord.status}) is NOT OK")
            throw OperationException("Agent状态异常")
        }

        logger.info("Trying to import the agent($agentId) of project($projectId) by user($userId)")
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val nodeId = nodeDao.addNode(
                context,
                projectId,
                agentRecord.ip,
                agentRecord.hostname,
                agentRecord.os.toLowerCase(),
                NodeStatus.NORMAL,
                NodeType.THIRDPARTY, userId
            )

            val maxNodeRecord = nodeDao.getMaxNodeStringId(context, projectId, nodeId)

            val maxNodeRecordId = if (maxNodeRecord == null) {
                0
            } else {
                val nodeStringId = maxNodeRecord.nodeStringId
                if (nodeStringId == null) {
                    0
                } else {
                    val split = nodeStringId.split("_")
                    if (split.size < 3) {
                        logger.warn("Unknown node string id format($nodeStringId)")
                        0
                    } else {
                        split[2].toInt()
                    }
                }
            }
            val nodeStringId = "BUILD_${HashUtil.encodeLongId(nodeId)}_${maxNodeRecordId + 1}"
            nodeDao.insertNodeStringIdAndDisplayName(context, nodeId, nodeStringId, nodeStringId)

            val count = thirdPartyAgentDao.updateStatus(context, id, nodeId, projectId, AgentStatus.IMPORT_OK)
            if (count != 1) {
                logger.warn("Fail to update the agent($id) to OK status")
                throw OperationException("Agent不存在")
            }
        }
    }

    /**
     * API FROM AGENT
     */
    fun agentStartup(
        projectId: String,
        agentId: String,
        secretKey: String,
        startInfo: ThirdPartyAgentStartInfo
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        logger.info("The agent($id) is start up by ${startInfo.hostIp}")
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId) ?: return AgentStatus.DELETE

        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            logger.warn("The secretKey($secretKey) is not match the expect one(${SecurityUtil.decrypt(agentRecord.secretKey)}) of id($id)")
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }

        val status = AgentStatus.fromStatus(agentRecord.status)
        if (!(AgentStatus.isImportException(status) || AgentStatus.isUnImport(status) || agentRecord.startRemoteIp.isNullOrBlank())) {
            if (startInfo.hostIp != agentRecord.startRemoteIp) {
                logger.warn("The agent is active and it's starting up in other host(${startInfo.hostIp}) from origin one(${agentRecord.startRemoteIp})")
                return AgentStatus.DELETE
            }
        }
        val updateCount = thirdPartyAgentDao.updateAgentInfo(
            dslContext,
            id,
            startInfo.hostIp,
            projectId,
            startInfo.hostname,
            startInfo.hostIp,
            startInfo.detectOS,
            startInfo.version,
            startInfo.masterVersion
        )
        if (updateCount != 1) {
            logger.warn("Fail to update the agent info($updateCount)")
        }
        return status
    }

    fun agentShutdown(
        projectId: String,
        agentId: String,
        secretKey: String,
        shutdownNormal: Boolean
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        logger.info("The agent($id) shutdown($shutdownNormal)")
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId) ?: return AgentStatus.DELETE

        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            logger.warn("The secretKey($secretKey) is not match the expect one(${SecurityUtil.decrypt(agentRecord.secretKey)}) of id($id)")
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }

        return AgentStatus.fromStatus(agentRecord.status)
    }

    fun getAgentStatus(
        projectId: String,
        agentId: String,
        secretKey: String
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId) ?: return AgentStatus.DELETE
        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            logger.warn("The secretKey($secretKey) is not match the expect one(${SecurityUtil.decrypt(agentRecord.secretKey)}) of id($id)")
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }
        return AgentStatus.fromStatus(agentRecord.status)
    }

    fun heartBeat(
        projectId: String,
        agentId: String,
        secretKey: String,
        heartbeatInfo: ThirdPartyAgentHeartbeatInfo?
    ): AgentStatus {
        logger.info("Agent($agentId) of project($projectId) heartbeat")
        val slaveVersion = if (heartbeatInfo != null) {
            heartbeatInfo.slaveVersion ?: ""
        } else {
            ""
        }
        val masterVersion = if (heartbeatInfo != null) {
            heartbeatInfo.masterVersion ?: ""
        } else {
            ""
        }
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val agentRecord = getAgentRecord(context, projectId, agentId, secretKey)

            if (agentRecord == null) {
                logger.warn("The agent($agentId) is not exist")
                return@transactionResult AgentStatus.DELETE
            }

            logger.info("agent version ${agentRecord.masterVersion}|$masterVersion|${agentRecord.version}|$slaveVersion")

            // 心跳上报版本号，且版本跟数据库对不上时，更新数据库
            if (!slaveVersion.isBlank() && !masterVersion.isBlank()) {
                if (agentRecord.version != slaveVersion || agentRecord.masterVersion != masterVersion) {
                    thirdPartyAgentDao.updateAgentVersion(
                        context,
                        agentRecord.id,
                        projectId,
                        slaveVersion,
                        masterVersion
                    )
                }
            }

            val status = AgentStatus.fromStatus(agentRecord.status)
            logger.info("Get the agent($agentId) status $status")

            val agentStatus = when {
                AgentStatus.isUnImport(status) -> {
                    logger.info("Update the agent($agentId) to un-import ok")
                    thirdPartyAgentDao.updateStatus(context, agentRecord.id, null, projectId, AgentStatus.UN_IMPORT_OK)
                    AgentStatus.UN_IMPORT_OK
                }
                AgentStatus.isImportException(status) -> {
                    logger.info("Update the agent($agentId) from exception to ok")
                    if (agentRecord.nodeId != null) {
                        nodeDao.updateNodeStatus(context, agentRecord.nodeId, NodeStatus.NORMAL)
                    }
                    AgentStatus.IMPORT_OK
                }
                else -> {
                    logger.info("Get the node id(${agentRecord.nodeId}) of agent($agentId)")
                    if (agentRecord.nodeId != null) {
                        val nodeRecord = nodeDao.get(context, projectId, agentRecord.nodeId)
                        if (nodeRecord == null) {
                            logger.warn("The node is not exist")
                            return@transactionResult AgentStatus.DELETE
                        }
                        if (nodeRecord.nodeStatus == NodeStatus.ABNORMAL.name) {
                            val count = nodeDao.updateNodeStatus(context, agentRecord.nodeId, NodeStatus.NORMAL)
                            logger.info("Update the node status - $count of agent $agentId")
                        }
                    }
                    status
                }
            }

            if (status == AgentStatus.IMPORT_OK) {
                if (agentRecord.nodeId == null) {
                    return@transactionResult AgentStatus.DELETE
                }
                val nodeRecord = nodeDao.get(context, projectId, agentRecord.nodeId)
                    ?: return@transactionResult AgentStatus.DELETE
                if (nodeRecord.nodeStatus == NodeStatus.DELETED.name) {
                    return@transactionResult AgentStatus.DELETE
                }
            }

            ThirdPartyAgentHeartbeatUtils.heartbeat(projectId, agentId, redisOperation)
            agentStatus
        }
    }

    private fun getAgentRecord(
        context: DSLContext,
        projectId: String,
        agentId: String,
        secretKey: String
    ): TEnvironmentThirdpartyAgentRecord? {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(context, id, projectId) ?: return null
        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            logger.warn("The secretKey($secretKey) is not match the expect one(${SecurityUtil.decrypt(agentRecord.secretKey)}) of id($id)")
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }
        return agentRecord
    }

    private fun generateSecretKey() = ApiUtil.randomSecretKey()

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentService::class.java)
    }
}
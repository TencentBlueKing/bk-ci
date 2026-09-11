package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.environment.TpaLock
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.AgentInstallSessionDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentInstallSession
import com.tencent.devops.environment.model.AgentInstallSessionMode
import com.tencent.devops.environment.model.AgentInstallSessionNode
import com.tencent.devops.environment.model.AgentInstallSessionNodeStatus
import com.tencent.devops.environment.model.AgentInstallSessionStatus
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeTagAddOrDeleteTagItem
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.thirdpartyagent.TPAInstallType
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.service.NodeTagService
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AgentInstallSessionRuntimeService(
    private val dslContext: DSLContext,
    private val sessionDao: AgentInstallSessionDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao,
    private val downloadAgentInstallService: DownloadAgentInstallService,
    private val importService: ImportService,
    private val nodeTagService: NodeTagService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val simpleRateLimiter: SimpleRateLimiter,
    private val redisOperation: RedisOperation
) {
    @Value("\${environment.batch-install.aes-key}")
    private var batchInstallAesKey: String = ""

    fun downloadInstallScript(token: String, os: OS): Response {
        val now = LocalDateTime.now()
        val session = sessionDao.getByTokenHash(dslContext, ShaUtils.sha256(token))
            ?: throw OperationException("Install session token is invalid")
        if (session.status != AgentInstallSessionStatus.ACTIVE || session.expiredTime <= now) {
            if (session.status == AgentInstallSessionStatus.ACTIVE) {
                sessionDao.markExpired(dslContext, session.id, now)
            }
            throw OperationException("Install session token is expired")
        }
        if (session.os != os.name) {
            throw OperationException("Install session operating system does not match")
        }
        checkExecutePermission(session)
        if (!simpleRateLimiter.acquire(ImportService.BU_SIZE, lockKey = "lock:tpa:session:rate:${session.id}")) {
            throw OperationException("Install session node count exceeds ${ImportService.BU_SIZE}")
        }

        val agentId = when (session.mode) {
            AgentInstallSessionMode.FIRST_IMPORT -> createAgent(session, os)
            AgentInstallSessionMode.REINSTALL -> validateReinstallAgent(session)
        }
        sessionDao.bindSessionAgent(
            dslContext = dslContext,
            node = AgentInstallSessionNode(
                sessionId = session.id,
                agentId = agentId,
                nodeId = session.targetNodeId,
                status = AgentInstallSessionNodeStatus.INSTALLING,
                startedTime = now,
                createdTime = now,
                updatedTime = now
            )
        )
        return downloadAgentInstallService.downloadInstallScript(
            agentId = HashUtil.encodeLongId(agentId),
            loginName = session.loginName,
            loginPassword = session.loginPasswordCipher?.takeIf { it.isNotBlank() }?.let {
                AESUtil.decrypt(batchInstallAesKey, it)
            },
            installType = TPAInstallType.valueOf(session.installType),
            allowInstalledAgent = session.mode == AgentInstallSessionMode.REINSTALL
        )
    }

    /**
     * Returns true when this agent belongs to an install session. Session agents are fully handled here, while legacy
     * agents continue through the original startup import branch.
     */
    fun processAgentStartup(
        projectId: String,
        agentHashId: String,
        startInfo: ThirdPartyAgentStartInfo
    ): Boolean {
        val agentId = HashUtil.decodeIdToLong(agentHashId)
        val sessionNode = sessionDao.findUnfinishedByAgentId(dslContext, agentId, LocalDateTime.now()) ?: return false
        val session = sessionDao.getById(dslContext, projectId, sessionNode.sessionId) ?: return false
        TpaLock(redisOperation, "install-session:apply:$agentId").use { lock ->
            if (!lock.tryLock()) {
                return true
            }
            val now = LocalDateTime.now()
            sessionDao.updateNodeStatus(
                dslContext,
                session.id,
                agentId,
                AgentInstallSessionNodeStatus.IMPORTING,
                now
            )
            sessionDao.updateNodeDetails(
                dslContext = dslContext,
                sessionId = session.id,
                agentId = agentId,
                nodeId = sessionNode.nodeId,
                hostname = startInfo.hostname,
                ip = startInfo.hostIp,
                errorMessage = null,
                agentVersion = startInfo.masterVersion ?: startInfo.version,
                updatedTime = now
            )
            var resolvedNodeId = sessionNode.nodeId
            try {
                if (session.mode == AgentInstallSessionMode.FIRST_IMPORT) {
                    importService.importAgent(
                        userId = session.createdBy,
                        projectId = projectId,
                        agentId = agentHashId,
                        masterVersion = startInfo.masterVersion
                    )
                }
                val agent = thirdPartyAgentDao.getAgentByProject(dslContext, agentId, projectId)
                    ?: throw OperationException("The session agent does not exist")
                val nodeId = agent.nodeId ?: throw OperationException("The session agent is not associated with a node")
                resolvedNodeId = nodeId
                if (session.mode == AgentInstallSessionMode.REINSTALL &&
                    (session.targetAgentId != agentId || session.targetNodeId != nodeId)
                ) {
                    throw OperationException("The reinstall session target does not match")
                }
                agent.parallelTaskCount = session.parallelTaskCount
                thirdPartyAgentDao.saveAgent(dslContext, agent)
                val tags = sessionDao.listTags(dslContext, session.id).map {
                    NodeTagAddOrDeleteTagItem(tagKeyId = it.tagKeyId, tagValueId = it.tagValueId)
                }
                nodeTagService.replaceUserTags(projectId, nodeId, tags)
                sessionDao.updateNodeDetails(
                    dslContext = dslContext,
                    sessionId = session.id,
                    agentId = agentId,
                    nodeId = nodeId,
                    hostname = startInfo.hostname,
                    ip = startInfo.hostIp,
                    errorMessage = null,
                    agentVersion = startInfo.masterVersion ?: startInfo.version,
                    updatedTime = LocalDateTime.now()
                )
                sessionDao.updateNodeStatus(
                    dslContext,
                    session.id,
                    agentId,
                    AgentInstallSessionNodeStatus.SUCCEEDED,
                    LocalDateTime.now()
                )
                return true
            } catch (e: Exception) {
                val failedAt = LocalDateTime.now()
                sessionDao.updateNodeDetails(
                    dslContext = dslContext,
                    sessionId = session.id,
                    agentId = agentId,
                    nodeId = resolvedNodeId,
                    hostname = startInfo.hostname,
                    ip = startInfo.hostIp,
                    errorMessage = (e.message ?: e.javaClass.simpleName).take(MAX_ERROR_MESSAGE_LENGTH),
                    agentVersion = startInfo.masterVersion ?: startInfo.version,
                    updatedTime = failedAt
                )
                sessionDao.updateNodeStatus(
                    dslContext,
                    session.id,
                    agentId,
                    AgentInstallSessionNodeStatus.FAILED,
                    failedAt
                )
                throw e
            }
        }
    }

    private fun createAgent(session: AgentInstallSession, os: OS): Long {
        val secretKey = ApiUtil.randomSecretKey()
        return thirdPartyAgentDao.add(
            dslContext = dslContext,
            userId = session.createdBy,
            projectId = session.projectId,
            os = os,
            secretKey = SecurityUtil.encrypt(secretKey),
            gateway = session.gateway,
            fileGateway = session.fileGateway,
            agentType = AgentType.valueOf(session.agentType),
            createWorkspaceName = null,
            agentProps = null,
            parallelTaskCount = session.parallelTaskCount
        )
    }

    private fun validateReinstallAgent(session: AgentInstallSession): Long {
        val agentId = session.targetAgentId ?: throw OperationException("Reinstall session has no target agent")
        val agent = thirdPartyAgentDao.getAgentByProject(dslContext, agentId, session.projectId)
            ?: throw OperationException("The reinstall agent does not exist")
        val nodeId = agent.nodeId ?: throw OperationException("The reinstall agent is not associated with a node")
        if (nodeId != session.targetNodeId) {
            throw OperationException("The reinstall session target does not match")
        }
        val node = nodeDao.get(dslContext, session.projectId, nodeId)
            ?: throw OperationException("The reinstall node does not exist")
        if (node.nodeStatus != NodeStatus.ABNORMAL.name) {
            throw OperationException("Only ABNORMAL nodes can be reinstalled")
        }
        return agentId
    }

    private fun checkExecutePermission(session: AgentInstallSession) {
        val allowed = if (session.mode == AgentInstallSessionMode.FIRST_IMPORT) {
            environmentPermissionService.checkNodePermission(
                userId = session.createdBy,
                projectId = session.projectId,
                permission = AuthPermission.CREATE
            )
        } else {
            val nodeId = session.targetNodeId ?: return
            environmentPermissionService.checkNodePermission(
                userId = session.createdBy,
                projectId = session.projectId,
                nodeId = nodeId,
                permission = AuthPermission.EDIT
            )
        }
        if (!allowed) {
            throw PermissionForbiddenException(message = "The install session creator no longer has permission")
        }
    }

    companion object {
        private const val MAX_ERROR_MESSAGE_LENGTH = 1024
    }
}

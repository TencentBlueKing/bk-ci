package com.tencent.devops.environment.service.thirdpartyagent

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.AgentDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeTagDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.thirdpartyagent.create.AgentPropsSource
import com.tencent.devops.environment.service.openworkspace.IegImateShareAccessService
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevInternalResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TencentAgentService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val agentDao: AgentDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val nodeTagDao: NodeTagDao,
    private val iegImateShareAccessService: IegImateShareAccessService
) {
    // TODO: 这里应该改成只拿实例ID，然后通过实例ID取各自的
    fun getWorkspaceInfo(
        userId: String,
        projectId: String,
        agentHashId: String
    ): WeSecProjectWorkspace? {
        val id = HashUtil.decodeIdToLong(agentHashId)
        val agentRecord =
            thirdPartyAgentDao.getAgentByProject(dslContext = dslContext, id = id, projectId = projectId) ?: return null
        return client.get(ServiceRemoteDevResource::class).getProjectWorkspace(
            userId = userId,
            projectId = projectId,
            workspaceName = agentRecord.createWorkspaceName ?: return null
        ).data
    }

    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_DELETE_CONTENT
    )
    fun deleteCreateNode(
        userId: String,
        projectId: String?,
        workspaceName: String
    ): Boolean {
        val record = agentDao.getAgentByWorkspaceIdGlobal(dslContext, workspaceName, projectId) ?: return true
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            if (record.nodeId != null) {
                nodeDao.batchDeleteNode(context, record.projectId, listOf(record.nodeId))
                envNodeDao.deleteByNodeIds(context, record.projectId, listOf(record.nodeId))
                environmentPermissionService.deleteNode(record.projectId, record.nodeId)
                // 删除节点相关标签
                nodeTagDao.deleteByNodes(context, listOf(record.nodeId))
            }
            agentDao.deleteAgentByWorkspaceIdGlobal(dslContext, workspaceName)
        }
        return true
    }

    fun updateCreateNodeProject(
        userId: String,
        newProjectId: String,
        workspaceName: String
    ): Boolean {
        logger.info(
            "updateCreateNodeProject start|userId=$userId|newProjectId=$newProjectId|workspaceName=$workspaceName"
        )
        val role = iegImateShareAccessService.getRole(
            userId = userId,
            workspaceName = workspaceName
        )
        if (!role.equals(IegImateShareAccessService.ADMIN_ROLE, ignoreCase = true)) {
            logger.warn(
                "updateCreateNodeProject denied|userId=$userId|workspaceName=$workspaceName|" +
                        "newProjectId=$newProjectId|role=$role"
            )
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_IMATE_NO_ADMIN_IMPORT_PERMISSION,
                defaultMessage = "只有分身管理员才能导入"
            )
        }
        val record = agentDao.getAgentByWorkspaceIdGlobal(dslContext, workspaceName, null)
        // 如果没有就新安装一个
        if (record == null || record.status == AgentStatus.UN_IMPORT.status) {
            if (record != null) {
                agentDao.updateProjectAndProps(
                    dslContext = dslContext,
                    agentId = record.id,
                    projectId = newProjectId,
                    props = JsonUtil.to(record.agentProps, object : TypeReference<AgentProps>() {})
                        .copy(source = AgentPropsSource.IEG_IMATE)
                )
            }
            createNewAgent(
                projectId = newProjectId,
                userId = userId,
                workspaceName = workspaceName,
                onlyInstall = record?.status == AgentStatus.UN_IMPORT.status,
                agentId = record?.id?.let { HashUtil.encodeLongId(it) },
                host = record?.gateway
            )
            return true
        }
        if (record.agentType != AgentType.CREATE.name) {
            logger.warn(
                "updateCreateNodeProject agent type invalid|userId=$userId|" +
                        "workspaceName=$workspaceName|agentType=${record.agentType}"
            )
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(workspaceName)
            )
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            if (record.nodeId != null) {
                // 原项目的都删除掉
                envNodeDao.deleteByNodeIds(context, record.projectId, listOf(record.nodeId))
                environmentPermissionService.deleteNode(record.projectId, record.nodeId)
                // 删除节点相关标签
                nodeTagDao.deleteByNodes(context, listOf(record.nodeId))

                // 修改项目信息
                nodeDao.updateProjectId(context, record.projectId, newProjectId, record.nodeId)
            }
            agentDao.updateProjectAndProps(
                dslContext = context,
                agentId = record.id,
                projectId = newProjectId,
                props = JsonUtil.to(record.agentProps, object : TypeReference<AgentProps>() {})
                    .copy(source = AgentPropsSource.IEG_IMATE)
            )
        }
        return true
    }

    @Value("\${environment.createAgentInstallUrl}")
    private val createAgentInstallUrl: String = ""

    private fun createNewAgent(
        projectId: String,
        userId: String,
        workspaceName: String,
        onlyInstall: Boolean,
        agentId: String?,
        host: String?
    ) {
        val detail = client.get(ServiceRemoteDevInternalResource::class).getWorkspaceDetail(workspaceName).data
            ?: throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(workspaceName)
            )
        val ip = detail.ip.substringAfter(".")
        val url = redisOperation.get(createAgentPipelineRedisKey, false) ?: createAgentInstallUrl
        OkhttpUtils.doPost(
            url,
            JsonUtil.toJson(
                mapOf(
                    "userId" to userId,
                    "ip" to ip,
                    "agent_id" to (agentId ?: ""),
                    "host" to (host ?: ""),
                    "project_id" to projectId,
                    "ONLY_INSTALL" to onlyInstall
                ), false
            ),
            mapOf(
                "Content-Type" to "application/json",
                "X-DEVOPS-PROJECT-ID" to "bkci-desktop",
                "X-DEVOPS-UID" to userId
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TencentAgentService::class.java)
        private val createAgentPipelineRedisKey = "environment:create_agent_install_url"
    }
}
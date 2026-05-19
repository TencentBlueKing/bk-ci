package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.dao.AgentDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeTagDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TencentAgentService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val agentDao: AgentDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val nodeTagDao: NodeTagDao
) {
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
        projectId: String,
        workspaceName: String
    ): Boolean {
        val record = agentDao.getAgentByWorkspaceIdGlobal(dslContext, workspaceName) ?: return true
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.batchDeleteNode(context, record.projectId, listOf(record.nodeId))
            envNodeDao.deleteByNodeIds(context, record.projectId, listOf(record.nodeId))
            environmentPermissionService.deleteNode(record.projectId, record.nodeId)
            // 删除节点相关标签
            nodeTagDao.deleteByNodes(dslContext, listOf(record.nodeId))
            agentDao.deleteAgentByWorkspaceIdGlobal(dslContext, workspaceName)
        }
        return true
    }
}
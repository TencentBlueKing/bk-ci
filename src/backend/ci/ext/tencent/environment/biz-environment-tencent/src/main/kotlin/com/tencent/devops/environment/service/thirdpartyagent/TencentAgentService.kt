package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TencentAgentService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao
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
}
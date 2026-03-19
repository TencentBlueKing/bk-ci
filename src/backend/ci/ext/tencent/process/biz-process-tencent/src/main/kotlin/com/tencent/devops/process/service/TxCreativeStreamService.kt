package com.tencent.devops.process.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.thirdpartyagent.ServiceAgentResource
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.WorkspaceBaseInfo
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TxCreativeStreamService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val pipelineRepositoryService: PipelineRepositoryService
) : CreativeStreamService(
    client = client,
    dslContext = dslContext,
    pipelineEventSubscriptionDao = pipelineEventSubscriptionDao,
    pipelineRepositoryService = pipelineRepositoryService
) {
    override fun getWorkspaceInfo(
        projectId: String,
        agentHashId: String,
        userId: String
    ) = try {
        client.get(ServiceAgentResource::class).getWorkspaceInfo(
            projectId = projectId,
            agentHashId = agentHashId,
            userId = userId
        ).data?.let {
            it.convertBaseInfo()
        }
    } catch (ignored: Exception) {
        logger.warn("failed to get workspace info", ignored)
        null
    }

    override fun getWorkspaceInfoByName(
        projectId: String,
        workspaceName: String,
        userId: String
    ) = try {
        client.get(ServiceRemoteDevResource::class).getProjectWorkspace(
            projectId = projectId,
            workspaceName = workspaceName,
            userId = userId
        ).data?.let {
            it.convertBaseInfo()
        }
    } catch (ignored: Exception) {
        logger.warn("failed to get workspace info", ignored)
        null
    }

    private fun WeSecProjectWorkspace.convertBaseInfo() =
        WorkspaceBaseInfo(
            projectId = projectId,
            innerIp = innerIp,
            displayName = displayName,
            workspaceName = workspaceName
        )

    companion object {
        private val logger = LoggerFactory.getLogger(TxCreativeStreamService::class.java)
    }
}
package com.tencent.devops.process.yaml.transfer

import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.TencentServiceNodeService
import com.tencent.devops.environment.api.thirdpartyagent.ServiceAgentResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TXWorkspaceAgentTransferService @Autowired constructor(
    private val client: Client
) : WorkspaceAgentTransferService() {

    override fun getWorkspaceNameByAgent(
        userId: String,
        projectId: String,
        agentHashId: String
    ): String? {
        return try {
            client.get(ServiceAgentResource::class).getWorkspaceInfo(
                projectId = projectId,
                agentHashId = agentHashId,
                userId = userId
            ).data?.workspaceName
        } catch (ignored: Exception) {
            logger.warn("failed to get workspace info", ignored)
            null
        }
    }

    override fun getAgentByWorkspaceName(
        userId: String,
        projectId: String,
        workspaceName: String
    ): String? {
        return try {
            client.get(TencentServiceNodeService::class).getNodeAgentDetailByWorkspaceName(
                userId = userId,
                projectId = projectId,
                workspaceName = workspaceName
            ).data?.agentHashId
        } catch (ignored: Exception) {
            logger.warn("failed to get workspace info", ignored)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TXWorkspaceAgentTransferService::class.java)
    }
}

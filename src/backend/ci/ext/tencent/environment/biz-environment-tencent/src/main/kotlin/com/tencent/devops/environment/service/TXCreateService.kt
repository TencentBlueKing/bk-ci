package com.tencent.devops.environment.service

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.model.AgentPropsSource
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.op.CreateAgent
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.service.thirdpartyagent.BatchInstallAgentService
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevInternalResource
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TXCreateService @Autowired constructor(
    private val client: Client,
    private val batchInstallAgentService: BatchInstallAgentService,
    private val slaveGatewayService: SlaveGatewayService,
    private val txCreateEnvService: TXCreateEnvService
) {
    fun genCreateAgent(
        userId: String,
        projectId: String,
        workspaceName: String
    ): CreateAgent? {
        val detail = client.get(ServiceRemoteDevInternalResource::class).getWorkspaceDetail(
            workspaceName
        ).data ?: return null
        val zoneName = txCreateEnvService.getCreateGatewayConfig()?.let { config ->
            when (detail.zoneConfig?.type) {
                WindowsResourceZoneConfigType.DEVCLOUD -> config.devnetGateway
                null -> null
                else -> config.devxGateway
            }
        } ?: return null
        val agentId = batchInstallAgentService.genNewAgent(
            projectId = projectId,
            userId = userId,
            os = detail.os ?: return null,
            zoneName = zoneName,
            agentType = AgentType.CREATE,
            createWorkspaceName = workspaceName,
            agentProps = AgentProps.emptyBySource(AgentPropsSource.REMOTEDEV)
        )
        return CreateAgent(
            host = slaveGatewayService.getGateway(zoneName) ?: "",
            agentId = HashUtil.encodeLongId(agentId)
        )
    }
}
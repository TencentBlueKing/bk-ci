package com.tencent.devops.environment.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.OpCreateResource
import com.tencent.devops.environment.pojo.op.CreateAgent
import com.tencent.devops.environment.pojo.op.CreateGateWayConfig
import com.tencent.devops.environment.service.TXCreateEnvService
import com.tencent.devops.environment.service.thirdpartyagent.TencentAgentService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpCreateResourceImpl @Autowired constructor(
    private val agentService: TencentAgentService,
    private val txEnvCreateService: TXCreateEnvService
) : OpCreateResource {
    override fun deleteNodes(userId: String, workspaceName: String) {
        agentService.deleteCreateNode(userId, null, workspaceName)
    }

    override fun refreshCreateNodeDisplayName(userId: String) {
        txEnvCreateService.refreshCreateDisplayName()
    }

    override fun getCreateInstallConfig(userId: String): CreateGateWayConfig? {
        return txEnvCreateService.getCreateGatewayConfig()
    }

    override fun setCreateInstallConfig(
        userId: String,
        data: CreateGateWayConfig
    ) {
        txEnvCreateService.setCreateGatewayConfig(data)
    }

    override fun addCreateNode(
        userId: String,
        projectId: String,
        workspaceName: String
    ): CreateAgent? {
        return txEnvCreateService.genCreateAgent(
            userId = userId,
            projectId = projectId,
            workspaceName = workspaceName
        )
    }
}
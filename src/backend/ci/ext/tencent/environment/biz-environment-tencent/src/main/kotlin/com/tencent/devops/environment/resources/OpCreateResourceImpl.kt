package com.tencent.devops.environment.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.OpCreateResource
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
}
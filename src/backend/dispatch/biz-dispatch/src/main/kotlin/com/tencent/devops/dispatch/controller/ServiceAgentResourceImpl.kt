package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.ServiceAgentResource
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.AgentBuildInfo
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAgentResourceImpl @Autowired constructor(val thirdPartyAgentService: ThirdPartyAgentService) : ServiceAgentResource {
    override fun listAgentBuild(agentId: String, page: Int?, pageSize: Int?): Page<AgentBuildInfo> {
        return thirdPartyAgentService.listAgentBuilds(agentId, page, pageSize)
    }
}
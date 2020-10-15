package com.tencent.devops.environment.resources.thirdPartyAgent

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdPartyAgent.BuildThirdPartyAgentResource
import com.tencent.devops.environment.service.thirdPartyAgent.ThirdPartyAgentMgrService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildThirdPartyAgentResourceImpl @Autowired constructor(
    private val thirdPartyAgentService: ThirdPartyAgentMgrService
) : BuildThirdPartyAgentResource {
    override fun getOs(userId: String, projectId: String, agentId: String): Result<String> {
        return Result(thirdPartyAgentService.getOs(userId, projectId, agentId))
    }
}
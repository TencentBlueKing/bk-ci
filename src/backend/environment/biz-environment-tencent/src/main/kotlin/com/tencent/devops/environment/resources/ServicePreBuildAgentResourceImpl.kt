package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdPartyAgent.ServicePreBuildAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.environment.service.thirdPartyAgent.PreBuildAgentMgrService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePreBuildAgentResourceImpl @Autowired constructor(
    private val preBuildAgentMgrService: PreBuildAgentMgrService
) : ServicePreBuildAgentResource {

    override fun createPrebuildAgent(userId: String, projectId: String, os: OS, zoneName: String?, initIp: String?): Result<ThirdPartyAgentStaticInfo> {
        return Result(preBuildAgentMgrService.createPrebuildAgent(userId, projectId, os, zoneName, initIp))
    }

    override fun listPreBuildAgent(userId: String, projectId: String, os: OS?): Result<List<ThirdPartyAgentStaticInfo>> {
        return Result(preBuildAgentMgrService.listPreBuildAgent(userId, projectId, os))
    }
}
package com.tencent.devops.environment.resources.thirdpartyagent

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdpartyagent.ServiceAgentResource
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace

@RestResource
class ServiceAgentResourceImpl : ServiceAgentResource {
    override fun getWorkspaceInfo(
        userId: String,
        projectId: String,
        agentHashId: String
    ): Result<WeSecProjectWorkspace?> {
        return Result(null)
    }
}
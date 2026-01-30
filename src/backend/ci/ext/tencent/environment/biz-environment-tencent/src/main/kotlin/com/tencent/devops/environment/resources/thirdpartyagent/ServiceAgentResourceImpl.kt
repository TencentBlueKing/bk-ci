package com.tencent.devops.environment.resources.thirdpartyagent

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdpartyagent.ServiceAgentResource
import com.tencent.devops.environment.service.thirdpartyagent.TencentAgentService
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAgentResourceImpl @Autowired constructor(
    private val agentService: TencentAgentService
) : ServiceAgentResource {
    override fun getWorkspaceInfo(
        userId: String,
        projectId: String,
        agentHashId: String
    ): Result<WeSecProjectWorkspace?> {
        return Result(agentService.getWorkspaceInfo(userId, projectId, agentHashId))
    }

    override fun updateDisplayNameByWorkspaceId(
        userId: String,
        projectId: String,
        workspaceId: String,
        displayName: String
    ) {
        agentService.updateDisplayNameByWorkspaceId(userId, projectId, workspaceId, displayName)
    }
}
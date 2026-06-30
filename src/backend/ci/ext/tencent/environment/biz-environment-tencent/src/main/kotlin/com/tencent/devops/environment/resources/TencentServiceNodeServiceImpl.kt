package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.TencentServiceNodeService
import com.tencent.devops.environment.pojo.NodeAgentDetail
import com.tencent.devops.environment.service.TencentNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class TencentServiceNodeServiceImpl @Autowired constructor(
    private val tencentNodeService: TencentNodeService
) : TencentServiceNodeService {
    override fun updateCreateNodeDisplay(
        userId: String,
        projectId: String,
        workspaceName: String,
        displayName: String
    ): Result<Boolean> {
        return Result(tencentNodeService.updateCreateNodeDisplay(userId, projectId, workspaceName, displayName))
    }

    override fun getNodeAgentDetail(userId: String, projectId: String, agentHashId: String): Result<NodeAgentDetail?> {
        return Result(tencentNodeService.getNodeAgentDetail(userId, projectId, agentHashId))
    }
}
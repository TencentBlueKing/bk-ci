package com.tencent.devops.environment.resources.tstack

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.tstack.UserTstackResource
import com.tencent.devops.environment.pojo.tstack.TstackNode
import com.tencent.devops.environment.service.tstack.TstackNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTstackResourceImpl @Autowired constructor(private val tstackNodeService: TstackNodeService) : UserTstackResource {
    override fun getVncToken(projectId: String, nodeHashId: String): Result<String> {
        return Result(tstackNodeService.getVncToken(projectId, nodeHashId))
    }

    override fun listAvailableTstackNodes(projectId: String): Result<List<TstackNode>> {
        return Result(tstackNodeService.listAvailableVm(projectId))
    }
}
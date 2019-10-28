package com.tencent.devops.environment.resources.tstack

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.tstack.ServiceTstackResource
import com.tencent.devops.environment.pojo.tstack.TstackNode
import com.tencent.devops.environment.service.tstack.TstackNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTstackResourceImpl @Autowired constructor(private val tstackNodeService: TstackNodeService) : ServiceTstackResource {
    override fun get(projectId: String, hashId: String): Result<TstackNode?> {
        return Result(tstackNodeService.get(projectId, hashId))
    }

    override fun updateNodeAvailable(hashId: String, available: Boolean): Result<Boolean> {
        tstackNodeService.updateAvailable(hashId, available)
        return Result(true)
    }

    override fun listAvailableTstackNodes(projectId: String): Result<List<TstackNode>> {
        return Result(tstackNodeService.listAvailableVm(projectId))
    }
}
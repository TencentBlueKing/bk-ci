package com.tencent.devops.environment.resources.tstack

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.tstack.OpTstackResource
import com.tencent.devops.environment.service.tstack.TstackNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpTstackResourceImpl @Autowired constructor(private val tstackNodeService: TstackNodeService) : OpTstackResource {
    override fun createTstackNode(): Result<String> {
        return Result(1, "还没实现")
    }

    override fun destroyTstackNode(stackNodeId: String): Result<Boolean> {
        return Result(1, "还没实现")
    }

    override fun assignTstackNode(projectId: String, stackNodeId: String, user: String): Result<String> {
        return Result(tstackNodeService.assignTstackNode(projectId, stackNodeId, user))
    }

    override fun unassignTstackNode(projectId: String, stackNodeId: String): Result<Boolean> {
        return Result(tstackNodeService.unassignTstackNode(projectId, stackNodeId))
    }
}
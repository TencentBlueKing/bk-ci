package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserImateResource
import com.tencent.devops.remotedev.pojo.WorkspaceAiInfo
import com.tencent.devops.remotedev.service.CoffeeAIService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImateResourceImpl @Autowired constructor(
    private val coffeeAIService: CoffeeAIService
) : UserImateResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserImateResourceImpl::class.java)
    }

    override fun listImateWorkspaces(userId: String): Result<List<WorkspaceAiInfo>> {
        logger.info("用户查询imate分身列表：userId={}", userId)
        return Result(coffeeAIService.getAiWorkspaceList(userId))
    }
}

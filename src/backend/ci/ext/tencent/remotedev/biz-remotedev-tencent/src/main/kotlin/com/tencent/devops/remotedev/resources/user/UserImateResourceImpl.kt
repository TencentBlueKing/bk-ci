package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserImateResource
import com.tencent.devops.remotedev.pojo.ImateWorkspaceInspect
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

    override fun inspectImateWorkspace(
        userId: String,
        workspaceName: String
    ): Result<ImateWorkspaceInspect> {
        if (workspaceName.isBlank()) {
            throw ParamBlankException("Invalid workspaceName")
        }
        logger.info("用户检查imate分身归属：userId={} workspaceName={}", userId, workspaceName)
        return Result(coffeeAIService.inspectUserWorkspace(userId, workspaceName))
    }

    override fun getImateToken(
        userId: String,
        workspaceName: String,
        expirationMinutes: Long?
    ): Result<String> {
        if (workspaceName.isBlank()) {
            throw ParamBlankException("Invalid workspaceName")
        }
        logger.info(
            "用户请求imate令牌：userId={} workspaceName={} expirationMinutes={}",
            userId,
            workspaceName,
            expirationMinutes
        )
        return Result(
            coffeeAIService.generateUserWorkspaceToken(
                userId = userId,
                workspaceName = workspaceName,
                expirationMinutes = expirationMinutes
            )
        )
    }
}

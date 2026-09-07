package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserImateResource
import com.tencent.devops.remotedev.pojo.ImateAvatar
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

    override fun listImateAvatars(userId: String): Result<List<ImateAvatar>> {
        logger.info("用户查询已登记数字分身：userId={}", userId)
        return Result(coffeeAIService.listImateAvatars(userId))
    }

    override fun inspectImateWorkspace(
        userId: String,
        workspaceName: String?,
        digitalAvatarId: String?
    ): Result<ImateWorkspaceInspect> {
        requireWorkspaceOrAvatar(workspaceName, digitalAvatarId)
        logger.info(
            "用户检查imate分身归属：userId={} workspaceName={} digitalAvatarId={}",
            userId,
            workspaceName,
            digitalAvatarId
        )
        return Result(
            coffeeAIService.inspectUserWorkspace(
                userId = userId,
                workspaceName = workspaceName,
                digitalAvatarId = digitalAvatarId
            )
        )
    }

    override fun getImateToken(
        userId: String,
        workspaceName: String?,
        expirationMinutes: Long?,
        digitalAvatarId: String?
    ): Result<String> {
        requireWorkspaceOrAvatar(workspaceName, digitalAvatarId)
        logger.info(
            "用户请求imate令牌：userId={} workspaceName={} digitalAvatarId={} expirationMinutes={}",
            userId,
            workspaceName,
            digitalAvatarId,
            expirationMinutes
        )
        return Result(
            coffeeAIService.generateUserWorkspaceToken(
                userId = userId,
                workspaceName = workspaceName,
                expirationMinutes = expirationMinutes,
                digitalAvatarId = digitalAvatarId
            )
        )
    }

    private fun requireWorkspaceOrAvatar(workspaceName: String?, digitalAvatarId: String?) {
        if (workspaceName.isNullOrBlank() && digitalAvatarId.isNullOrBlank()) {
            throw ParamBlankException("Invalid workspaceName")
        }
    }
}

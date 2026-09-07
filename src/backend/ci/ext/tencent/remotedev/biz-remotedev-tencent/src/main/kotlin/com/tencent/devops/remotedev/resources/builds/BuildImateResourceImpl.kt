package com.tencent.devops.remotedev.resources.builds

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import com.tencent.devops.remotedev.api.builds.BuildImateResource
import com.tencent.devops.remotedev.pojo.ImateAvatar
import com.tencent.devops.remotedev.pojo.ImateTokenSession
import com.tencent.devops.remotedev.pojo.ImateWorkspaceInspect
import com.tencent.devops.remotedev.pojo.WorkspaceAiInfo
import com.tencent.devops.remotedev.service.CoffeeAIService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildImateResourceImpl @Autowired constructor(
    private val coffeeAIService: CoffeeAIService
) : BuildImateResource {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildImateResourceImpl::class.java)
    }

    @SensitiveApiPermission("get_remotedev_x_user_token")
    override fun getUserToken(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        username: String,
        workspaceName: String?,
        expirationMinutes: Long?,
        digitalAvatarId: String?
    ): Result<String> {
        validateBuildRequest(
            projectId = projectId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            username = username,
            workspaceName = workspaceName,
            digitalAvatarId = digitalAvatarId,
            requireWorkspaceTarget = true
        )
        return try {
            logger.info(
                "构建请求imate用户令牌：username={} workspaceName={} digitalAvatarId={} projectId={} " +
                    "buildId={} vmSeqId={} expirationMinutes={}",
                username,
                workspaceName,
                digitalAvatarId,
                projectId,
                buildId,
                vmSeqId,
                expirationMinutes
            )
            Result(
                coffeeAIService.generateBuildUserToken(
                    projectId = projectId,
                    buildId = buildId,
                    username = username,
                    workspaceName = workspaceName,
                    expirationMinutes = expirationMinutes,
                    digitalAvatarId = digitalAvatarId
                )
            )
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: ParamBlankException) {
            throw e
        } catch (e: Exception) {
            logger.error(
                "生成imate用户令牌失败：username={} workspaceName={} digitalAvatarId={} projectId={} buildId={}",
                username,
                workspaceName,
                digitalAvatarId,
                projectId,
                buildId,
                e
            )
            Result(status = 1, message = "生成令牌失败: ${e.message}")
        }
    }

    @SensitiveApiPermission("get_remotedev_x_user_token")
    override fun getUserSession(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        username: String,
        workspaceName: String?,
        expirationMinutes: Long?,
        digitalAvatarId: String?
    ): Result<ImateTokenSession> {
        validateBuildRequest(
            projectId = projectId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            username = username,
            workspaceName = workspaceName,
            digitalAvatarId = digitalAvatarId,
            requireWorkspaceTarget = true
        )
        return try {
            logger.info(
                "构建请求imate会话凭据：username={} workspaceName={} digitalAvatarId={} projectId={} " +
                    "buildId={} vmSeqId={} expirationMinutes={}",
                username,
                workspaceName,
                digitalAvatarId,
                projectId,
                buildId,
                vmSeqId,
                expirationMinutes
            )
            Result(
                coffeeAIService.generateBuildUserSession(
                    projectId = projectId,
                    buildId = buildId,
                    username = username,
                    workspaceName = workspaceName,
                    expirationMinutes = expirationMinutes,
                    digitalAvatarId = digitalAvatarId
                )
            )
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: ParamBlankException) {
            throw e
        } catch (e: Exception) {
            logger.error(
                "生成imate会话凭据失败：username={} workspaceName={} digitalAvatarId={} projectId={} buildId={}",
                username,
                workspaceName,
                digitalAvatarId,
                projectId,
                buildId,
                e
            )
            Result(status = 1, message = "生成令牌失败: ${e.message}")
        }
    }

    @SensitiveApiPermission("get_remotedev_x_user_token")
    override fun inspectWorkspace(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        username: String,
        workspaceName: String?,
        digitalAvatarId: String?
    ): Result<ImateWorkspaceInspect> {
        validateBuildRequest(
            projectId = projectId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            username = username,
            workspaceName = workspaceName,
            digitalAvatarId = digitalAvatarId,
            requireWorkspaceTarget = true
        )
        return Result(
            coffeeAIService.inspectBuildWorkspace(
                projectId = projectId,
                buildId = buildId,
                username = username,
                workspaceName = workspaceName,
                digitalAvatarId = digitalAvatarId
            )
        )
    }

    @SensitiveApiPermission("get_remotedev_x_user_token")
    override fun listAvatars(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        username: String
    ): Result<List<ImateAvatar>> {
        validateBuildRequest(
            projectId = projectId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            username = username,
            workspaceName = null,
            digitalAvatarId = null,
            requireWorkspaceTarget = false
        )
        logger.info(
            "构建查询已登记数字分身：username={} projectId={} buildId={} vmSeqId={}",
            username,
            projectId,
            buildId,
            vmSeqId
        )
        return Result(
            coffeeAIService.listBuildAvatars(
                projectId = projectId,
                buildId = buildId,
                username = username
            )
        )
    }

    @SensitiveApiPermission("get_remotedev_x_user_token")
    override fun listWorkspaces(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        username: String
    ): Result<List<WorkspaceAiInfo>> {
        validateBuildRequest(
            projectId = projectId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            username = username,
            workspaceName = null,
            digitalAvatarId = null,
            requireWorkspaceTarget = false
        )
        logger.info(
            "构建查询imate分身列表：username={} projectId={} buildId={} vmSeqId={}",
            username,
            projectId,
            buildId,
            vmSeqId
        )
        return Result(
            coffeeAIService.listBuildWorkspaces(
                projectId = projectId,
                buildId = buildId,
                username = username
            )
        )
    }

    private fun validateBuildRequest(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        username: String,
        workspaceName: String?,
        digitalAvatarId: String?,
        requireWorkspaceTarget: Boolean
    ) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (vmSeqId.isBlank()) {
            throw ParamBlankException("Invalid vmSeqId")
        }
        if (username.isBlank()) {
            throw ParamBlankException("Invalid username")
        }
        if (requireWorkspaceTarget && workspaceName.isNullOrBlank() && digitalAvatarId.isNullOrBlank()) {
            throw ParamBlankException("Invalid workspaceName")
        }
    }
}

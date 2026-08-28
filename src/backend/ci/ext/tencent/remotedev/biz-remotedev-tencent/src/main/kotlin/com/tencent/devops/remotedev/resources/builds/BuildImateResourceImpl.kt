package com.tencent.devops.remotedev.resources.builds

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import com.tencent.devops.remotedev.api.builds.BuildImateResource
import com.tencent.devops.remotedev.pojo.ImateWorkspaceInspect
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
        workspaceName: String
    ): Result<String> {
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
        if (workspaceName.isBlank()) {
            throw ParamBlankException("Invalid workspaceName")
        }
        return try {
            logger.info(
                "构建请求imate用户令牌：username={} workspaceName={} projectId={} buildId={} vmSeqId={}",
                username,
                workspaceName,
                projectId,
                buildId,
                vmSeqId
            )
            Result(
                coffeeAIService.generateBuildUserToken(
                    projectId = projectId,
                    buildId = buildId,
                    username = username,
                    workspaceName = workspaceName
                )
            )
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: ParamBlankException) {
            throw e
        } catch (e: Exception) {
            logger.error(
                "生成imate用户令牌失败：username={} workspaceName={} projectId={} buildId={}",
                username,
                workspaceName,
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
        workspaceName: String
    ): Result<ImateWorkspaceInspect> {
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
        if (workspaceName.isBlank()) {
            throw ParamBlankException("Invalid workspaceName")
        }
        return Result(
            coffeeAIService.inspectBuildWorkspace(
                projectId = projectId,
                buildId = buildId,
                username = username,
                workspaceName = workspaceName
            )
        )
    }
}

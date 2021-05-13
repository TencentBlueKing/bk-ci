package com.tencent.devops.auth.service.gitci

import com.tencent.devops.auth.service.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class GitCIPermissionServiceImpl @Autowired constructor(
    val client: Client
): PermissionService {
    // GitCI权限场景不会出现次调用, 故做默认实现
    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        return true
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        // 操作类action需要校验用户oauth, 查看类的无需oauth校验
        if (!checkListOrViewAction(action)) {
            client.get(ServiceOauthResource::class).gitGet(userId).data ?: return false
        }

        val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)
        logger.info("GitCICertPermissionServiceImpl user:$userId projectId: $projectCode gitProject: $gitProjectId")
        return client.get(ServiceGitCiResource::class).checkUserGitAuth(userId, gitProjectId).data ?: false
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        return validateUserResourcePermission(userId, action, projectCode, resourceCode)
    }

    // GitCI权限场景不会出现次调用, 故做默认实现
    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        return emptyList()
    }

    // GitCI权限场景不会出现次调用, 故做默认实现
    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        return emptyMap()
    }

    private fun checkListOrViewAction(action: String) : Boolean {
        if (action.contains(AuthPermission.LIST.value) || action.contains(AuthPermission.VIEW.value)) {
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(GitCIPermissionServiceImpl::class.java)
    }
}

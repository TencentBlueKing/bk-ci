package com.tencent.devops.auth.service.gitci

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class GitCIPermissionServiceImpl @Autowired constructor(
    val client: Client
) : PermissionService {

    private val gitCIUserCache = CacheBuilder.newBuilder()
        .maximumSize(2000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String/*userId*/, String>()

    private val projectPublicCache = CacheBuilder.newBuilder()
        .maximumSize(2000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String/*project*/, String?>()

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
            val checkOauth = client.get(ServiceOauthResource::class).gitGet(userId).data
            if (checkOauth == null) {
                logger.warn("GitCICertPermissionServiceImpl $userId oauth is empty")
                throw OauthForbiddenException("oauth is empty")
            }
        }
        logger.info("GitCICertPermissionServiceImpl user:$userId projectId: $projectCode")

        // 若为开源项目,则鉴权全部放行
        if (checkProjectPublic(projectCode)) {
            return true
        }

        val gitUserId = getGitUserByRtx(userId, projectCode)
        if (gitUserId.isNullOrEmpty()) {
            logger.warn("$userId is not gitCI user")
            return false
        }

        return client.getScm(ServiceGitCiResource::class).checkUserGitAuth(gitUserId, projectCode).data ?: false
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

    private fun getGitUserByRtx(rtxUserId: String, projectCode: String): String? {
        return if (!gitCIUserCache.getIfPresent(rtxUserId).isNullOrEmpty()) {
            gitCIUserCache.getIfPresent(rtxUserId)!!
        } else {
            val gitUserId = client.getScm(ServiceGitCiResource::class).getGitUserId(rtxUserId, projectCode).data
            if (gitUserId != null) {
                gitCIUserCache.put(rtxUserId, gitUserId)
            }
            gitUserId
        }
    }

    private fun checkProjectPublic(projectCode: String): Boolean {
        if (!projectPublicCache.getIfPresent(projectCode).isNullOrEmpty()) {
            return true
        } else {
            val gitProjectInfo = client.getScm(ServiceGitCiResource::class).getGitCodeProjectInfo(projectCode).data
            if (gitProjectInfo != null) {
                logger.info("project $projectCode visibilityLevel: ${gitProjectInfo?.visibilityLevel}")
                if (gitProjectInfo.visibilityLevel != null && gitProjectInfo.visibilityLevel!! > 0) {
                    projectPublicCache.put(projectCode, gitProjectInfo.visibilityLevel.toString())
                    return true
                }
            } else {
                logger.warn("project $projectCode get projectInfo is empty")
            }
        }
        return false
    }

    private fun checkListOrViewAction(action: String): Boolean {
        if (action.contains(AuthPermission.LIST.value) || action.contains(AuthPermission.VIEW.value)) {
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(GitCIPermissionServiceImpl::class.java)
    }
}

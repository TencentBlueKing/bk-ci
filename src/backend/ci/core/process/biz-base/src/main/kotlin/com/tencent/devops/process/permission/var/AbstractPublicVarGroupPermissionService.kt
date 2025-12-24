package com.tencent.devops.process.permission.`var`

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.PublicVarGroupAuthServiceCode
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.`var`.PublicVarGroupPermissions
import org.slf4j.LoggerFactory

abstract class AbstractPublicVarGroupPermissionService constructor(
    val authProjectApi: AuthProjectApi,
    val publicVarGroupAuthServiceCode: PublicVarGroupAuthServiceCode
) : PublicVarGroupPermissionService {

    override fun checkPublicVarGroupPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        groupName: String
    ): Boolean {
        return authProjectApi.checkProjectManager(
            userId = userId,
            serviceCode = publicVarGroupAuthServiceCode,
            projectCode = projectId
        )
    }

    override fun checkPublicVarGroupPermissionWithMessage(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        groupName: String
    ): Boolean {
        if (!checkPublicVarGroupPermission(
                userId = userId,
                projectId = projectId,
                permission = permission,
                groupName = groupName
            )) {
            logger.warn("User $userId has no permission to ${permission.value} var group in project $projectId")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                params = arrayOf(userId, projectId)
            )
        }
        return true
    }

    override fun checkPublicVarGroupPermissions(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean {
        return authProjectApi.checkProjectManager(
            userId = userId,
            serviceCode = publicVarGroupAuthServiceCode,
            projectCode = projectId
        )
    }

    override fun getPublicVarGroupPermissions(
        userId: String,
        projectId: String,
        groupName: String
    ): PublicVarGroupPermissions {
        val isManager = authProjectApi.checkProjectManager(
            userId = userId,
            serviceCode = publicVarGroupAuthServiceCode,
            projectCode = projectId
        )

        return if (isManager) {
            // 管理员拥有所有权限
            PublicVarGroupPermissions(
                canEdit = true,
                canView = true,
                canDelete = true,
                canUse = true
            )
        } else {
            // 非管理员默认只有查看和使用权限
            PublicVarGroupPermissions(
                canEdit = false,
                canView = true,
                canDelete = false,
                canUse = true
            )
        }
    }

    override fun filterPublicVarGroups(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>,
        groupNames: List<String>
    ): Map<AuthPermission, List<String>> {
        val isManager = authProjectApi.checkProjectManager(
            userId = userId,
            serviceCode = publicVarGroupAuthServiceCode,
            projectCode = projectId
        )

        return if (isManager) {
            authPermissions.associateWith { groupNames }
        } else {
            authPermissions.associateWith { permission ->
                when (permission) {
                    AuthPermission.VIEW, AuthPermission.USE -> groupNames
                    else -> emptyList()
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractPublicVarGroupPermissionService::class.java)
    }
}

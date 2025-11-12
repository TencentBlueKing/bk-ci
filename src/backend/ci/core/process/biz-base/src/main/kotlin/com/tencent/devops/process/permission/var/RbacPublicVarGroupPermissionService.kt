package com.tencent.devops.process.permission.`var`

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PublicVarGroupAuthServiceCode
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.`var`.PublicVarGroupPermissions
import org.slf4j.LoggerFactory

@Suppress("LongParameterList")
class RbacPublicVarGroupPermissionService constructor(
    val authPermissionApi: AuthPermissionApi,
    val authResourceApi: AuthResourceApi,
    authProjectApi: AuthProjectApi,
    publicVarGroupAuthServiceCode: PublicVarGroupAuthServiceCode
) : AbstractPublicVarGroupPermissionService(
    authProjectApi = authProjectApi,
    publicVarGroupAuthServiceCode = publicVarGroupAuthServiceCode
) {

    override fun checkPublicVarGroupPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        groupName: String
    ): Boolean {

        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = publicVarGroupAuthServiceCode,
            resourceType = RESOURCE_TYPE,
            projectCode = projectId,
            resourceCode = groupName,
            permission = permission
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
            logger.warn(
                "User($userId) does not have permission to ${permission.value} " +
                        "var group under project($projectId)"
            )
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                defaultMessage = "用户无权限操作公共变量组"
            )
        }
        return true
    }

    override fun getPublicVarGroupPermissions(
        userId: String,
        projectId: String,
        groupName: String
    ): PublicVarGroupPermissions {
        // 逐个检查权限
        val canEdit = checkPublicVarGroupPermission(
            userId, projectId, AuthPermission.EDIT, groupName
        )
        val canView = checkPublicVarGroupPermission(
            userId, projectId, AuthPermission.VIEW, groupName
        )
        val canDelete = checkPublicVarGroupPermission(
            userId, projectId, AuthPermission.DELETE, groupName
        )
        val canUse = checkPublicVarGroupPermission(
            userId, projectId, AuthPermission.USE, groupName
        )

        return PublicVarGroupPermissions(
            canEdit = canEdit,
            canView = canView,
            canDelete = canDelete,
            canUse = canUse
        )
    }

    override fun getResourcesByPermission(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        return permissions.associateWith {
            authPermissionApi.getUserResourceByPermission(
                user = userId,
                serviceCode = publicVarGroupAuthServiceCode,
                resourceType = RESOURCE_TYPE,
                projectCode = projectId,
                permission = it,
                supplier = null
            )
        }
    }

    override fun createResource(
        userId: String,
        projectId: String,
        groupCode: String,
        name: String
    ) {
        authResourceApi.createResource(
            user = userId,
            projectCode = projectId,
            serviceCode = publicVarGroupAuthServiceCode,
            resourceType = RESOURCE_TYPE,
            resourceCode = groupCode,
            resourceName = name
        )
    }
    

    override fun deleteResource(projectId: String, groupName: String) {
        authResourceApi.deleteResource(
            serviceCode = publicVarGroupAuthServiceCode,
            resourceType = RESOURCE_TYPE,
            projectCode = projectId,
            resourceCode = groupName
        )
    }

    companion object {
        private val RESOURCE_TYPE = AuthResourceType.PUBLIC_VAR_GROUP
        private val logger = LoggerFactory.getLogger(RbacPublicVarGroupPermissionService::class.java)
    }
}

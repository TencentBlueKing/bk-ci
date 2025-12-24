package com.tencent.devops.process.permission.`var`

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
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
                params = arrayOf(userId, projectId)
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
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            groupName = groupName
        )
        val canView = checkPublicVarGroupPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            groupName = groupName
        )
        val canDelete = checkPublicVarGroupPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.DELETE,
            groupName = groupName
        )
        val canUse = checkPublicVarGroupPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.USE,
            groupName = groupName
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

    override fun filterPublicVarGroups(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>,
        groupNames: List<String>
    ): Map<AuthPermission, List<String>> {
        logger.info("[rbac] filter public var groups|$userId|$projectId|$authPermissions")
        val startEpoch = System.currentTimeMillis()
        try {
            val resources = publicVarGroups2AuthResources(
                projectId = projectId,
                groupNames = groupNames
            )
            return authPermissionApi.filterResourcesByPermissions(
                user = userId,
                serviceCode = publicVarGroupAuthServiceCode,
                resourceType = RESOURCE_TYPE,
                projectCode = projectId,
                permissions = authPermissions,
                resources = resources
            )
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to filter public var groups|" +
                    "$userId|$projectId|$authPermissions"
            )
        }
    }

    private fun publicVarGroups2AuthResources(
        projectId: String,
        groupNames: List<String>
    ): List<AuthResourceInstance> {
        val projectInstance = AuthResourceInstance(
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        return groupNames.map { groupName ->
            AuthResourceInstance(
                resourceType = RESOURCE_TYPE.value,
                resourceCode = groupName,
                parents = listOf(projectInstance)
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

    override fun checkPublicVarGroupPermissions(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean {
        val resourcePermission = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = publicVarGroupAuthServiceCode,
            resourceType = RESOURCE_TYPE,
            projectCode = projectId,
            resourceCode = projectId,
            permission = permission
        )
        if (!resourcePermission) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                params = arrayOf(userId, projectId)
            )
        }
        return resourcePermission
    }

    companion object {
        private val RESOURCE_TYPE = AuthResourceType.PUBLIC_VAR_GROUP
        private val logger = LoggerFactory.getLogger(RbacPublicVarGroupPermissionService::class.java)
    }
}

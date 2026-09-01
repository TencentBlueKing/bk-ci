package com.tencent.devops.process.permission.`var`

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PublicVarGroupAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.`var`.PublicVarGroupPermissions
import org.slf4j.LoggerFactory

@Suppress("LongParameterList")
class RbacPublicVarGroupPermissionService(
    val authResourceApi: AuthResourceApi,
    val client: Client,
    val tokenService: ClientTokenService,
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
        val action = AuthResourceType.PUBLIC_VAR_GROUP.value + "_" + permission.value
        return client.get(ServicePermissionAuthResource::class)
            .validateUserResourcePermissionByRelation(
                token = tokenService.getSystemToken(),
                userId = userId,
                projectCode = projectId,
                resourceType = RESOURCE_TYPE.value,
                resourceCode = groupName,
                relationResourceType = null,
                action = action
            ).data ?: false
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
                        "var group($groupName) under project($projectId)"
            )
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_NO_PERMISSION,
                params = arrayOf(groupName, permission.getI18n(I18nUtil.getLanguage()))
            )
        }
        return true
    }

    override fun filterPublicVarGroups(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        logger.info("[rbac] filter public var groups|$userId|$projectId|$authPermissions")
        val startEpoch = System.currentTimeMillis()
        try {
            val actions = authPermissions.map { permission ->
                AuthResourceType.PUBLIC_VAR_GROUP.value + "_" + permission.value
            }
            // key 类型由接口签名强限制（服务端按 action 后缀反查枚举），无需转换，与全项目 RBAC 调用方一致
            return client.get(ServicePermissionAuthResource::class)
                .getUserResourcesByPermissions(
                    token = tokenService.getSystemToken(),
                    userId = userId,
                    projectCode = projectId,
                    action = actions,
                    resourceType = RESOURCE_TYPE.value
                ).data ?: emptyMap()
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to filter public var groups|" +
                    "$userId|$projectId|$authPermissions"
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

    override fun checkPublicVarGroupCreatePermission(
        userId: String,
        projectId: String
    ): Boolean {
        val resourcePermission =
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                token = tokenService.getSystemToken(),
                userId = userId,
                projectCode = projectId,
                relationResourceType = null,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectId,
                action = AuthResourceType.PUBLIC_VAR_GROUP.value + "_" + AuthPermission.CREATE.value,
            ).data ?: false
        if (!resourcePermission) {
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
        val resourcePermission =
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                token = tokenService.getSystemToken(),
                userId = userId,
                projectCode = projectId,
                relationResourceType = null,
                resourceType = AuthResourceType.PUBLIC_VAR_GROUP.value,
                resourceCode = "*",
                action = AuthResourceType.PUBLIC_VAR_GROUP.value + "_" + permission.value,
            ).data ?: false
        if (!resourcePermission) {
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
        val permissionMap = filterPublicVarGroups(
            userId = userId,
            projectId = projectId,
            authPermissions = setOf(
                AuthPermission.EDIT,
                AuthPermission.VIEW,
                AuthPermission.DELETE,
                AuthPermission.USE
            )
        )
        return PublicVarGroupPermissions(
            canEdit = permissionMap[AuthPermission.EDIT]?.contains(groupName) ?: false,
            canView = permissionMap[AuthPermission.VIEW]?.contains(groupName) ?: false,
            canDelete = permissionMap[AuthPermission.DELETE]?.contains(groupName) ?: false,
            canUse = permissionMap[AuthPermission.USE]?.contains(groupName) ?: false
        )
    }

    companion object {
        private val RESOURCE_TYPE = AuthResourceType.PUBLIC_VAR_GROUP
        private val logger = LoggerFactory.getLogger(RbacPublicVarGroupPermissionService::class.java)
    }
}

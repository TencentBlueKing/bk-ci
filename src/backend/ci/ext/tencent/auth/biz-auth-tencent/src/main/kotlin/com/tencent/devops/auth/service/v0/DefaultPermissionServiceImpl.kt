package com.tencent.devops.auth.service.v0

import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.BSCommonAuthServiceCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class DefaultPermissionServiceImpl @Autowired constructor(
    private val authPermissionApi: AuthPermissionApi,
    val authServiceCode: BSCommonAuthServiceCode
) : PermissionService {

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        return false
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = authServiceCode,
            resourceType = AuthResourceType.get(resourceType!!),
            projectCode = projectCode,
            permission = AuthPermission.get(action)
        )
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = authServiceCode,
            resourceType = AuthResourceType.get(resourceType!!),
            projectCode = projectCode,
            permission = AuthPermission.get(action),
            resourceCode = resourceCode,
            relationResourceType = null
        )
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            projectCode = projectCode,
            resourceType = AuthResourceType.get(resourceType),
            serviceCode = authServiceCode,
            supplier = null,
            permission = AuthPermission.get(action)
        )
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        val permissions = mutableSetOf<AuthPermission>()
        actions.forEach {
            permissions.add(AuthPermission.get(it))
        }
        return authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = authServiceCode,
            resourceType = AuthResourceType.get(resourceType),
            projectCode = projectCode,
            permissions = permissions,
            supplier = null
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(DefaultPermissionServiceImpl::class.java)
    }
}

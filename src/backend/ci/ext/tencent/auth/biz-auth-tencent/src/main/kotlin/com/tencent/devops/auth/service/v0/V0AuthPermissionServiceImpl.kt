package com.tencent.devops.auth.service.v0

import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V0AuthPermissionServiceImpl @Autowired constructor(
    private val authPermissionApi: AuthPermissionApi,
    val serviceCodeService: ServiceCodeService
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
        val serviceCodeService = serviceCodeService.getServiceCodeByResource(resourceType!!)
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = serviceCodeService,
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
        val serviceCodeService = serviceCodeService.getServiceCodeByResource(resourceType!!)
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = serviceCodeService,
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
        val serviceCodeService = serviceCodeService.getServiceCodeByResource(resourceType!!)
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            projectCode = projectCode,
            resourceType = AuthResourceType.get(resourceType),
            serviceCode = serviceCodeService,
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
        val serviceCodeService = serviceCodeService.getServiceCodeByResource(resourceType!!)

        val permissions = mutableSetOf<AuthPermission>()
        actions.forEach {
            permissions.add(AuthPermission.get(it))
        }
        return authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = serviceCodeService,
            resourceType = AuthResourceType.get(resourceType),
            projectCode = projectCode,
            permissions = permissions,
            supplier = null
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(V0AuthPermissionServiceImpl::class.java)
    }
}

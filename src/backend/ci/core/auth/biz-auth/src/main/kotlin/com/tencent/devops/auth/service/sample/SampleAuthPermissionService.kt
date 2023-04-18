package com.tencent.devops.auth.service.sample

import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import org.slf4j.LoggerFactory

class SampleAuthPermissionService : PermissionService {
    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        return true
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        return true
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        return true
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceCode: String,
        resourceType: String
    ): Map<String, Boolean> {
        return actions.associateWith { action ->
            validateUserResourcePermissionByRelation(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceCode = resourceCode,
                resourceType = resourceType,
                relationResourceType = null
            )
        }
    }

    override fun batchValidateUserResourcePermissionByInstance(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resource: AuthResourceInstance
    ): Map<String, Boolean> {
        return actions.associateWith { action ->
            validateUserResourcePermissionByRelation(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceCode = resource.resourceCode,
                resourceType = resource.resourceType,
                relationResourceType = null
            )
        }
    }

    override fun validateUserResourcePermissionByInstance(
        userId: String,
        action: String,
        projectCode: String,
        resource: AuthResourceInstance
    ): Boolean {
        return true
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        logger.info("getUserResourceByAction $userId $action $projectCode $resourceType")
        return emptyList()
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        return emptyMap()
    }

    override fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resources: List<AuthResourceInstance>
    ): Map<AuthPermission, List<String>> {
        return actions.associate { action ->
            val authPermission = action.substringAfterLast("_")
            AuthPermission.get(authPermission) to resources.map { it.resourceCode }
        }
    }

    override fun getUserResourceAndParentByPermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): Map<String, List<String>> {
        return emptyMap()
    }

    companion object {
        val logger = LoggerFactory.getLogger(SampleAuthPermissionService::class.java)
    }
}

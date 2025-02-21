package com.tencent.devops.auth.service

import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import org.springframework.stereotype.Service

@Service
class BkPermissionService(
    private val permissionManageFacadeService: PermissionManageFacadeService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService
) {
    fun validateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        action: String
    ): Boolean {
        val groupIds = permissionManageFacadeService.listMemberGroupIdsInProject(projectCode, userId)
        return permissionResourceGroupPermissionService.isGroupsHasProjectLevelPermission(
            projectCode = projectCode,
            filterIamGroupIds = groupIds,
            action = action
        )
    }

    fun validateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        action: String,
    ): Boolean {
        val groupIds = permissionManageFacadeService.listMemberGroupIdsInProject(projectCode, userId)
        return permissionResourceGroupPermissionService.isGroupsHasPermission(
            projectCode = projectCode,
            filterIamGroupIds = groupIds,
            relatedResourceType = resourceType,
            relatedResourceCode = resourceCode,
            action = action
        )
    }

    fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        actions: List<String>
    ): Map<String, Boolean> {
        return actions.associateWith { action ->
            validateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action
            )
        }
    }

    fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        val groupIds = permissionManageFacadeService.listMemberGroupIdsInProject(projectCode, userId)
        return permissionResourceGroupPermissionService.listResourcesWithPermission(
            projectCode = projectCode,
            filterIamGroupIds = groupIds,
            relatedResourceType = resourceType,
            action = action
        )
    }

    fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<String, List<String>> {
        return actions.associateWith {
            getUserResourceByAction(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceType = resourceType
            )
        }
    }
}

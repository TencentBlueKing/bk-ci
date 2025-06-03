package com.tencent.devops.auth.service

import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import org.springframework.stereotype.Service

/**
 * 蓝盾内部权限类，非第三方接口。
 * */
@Service
class BkInternalPermissionService(
    private val permissionManageFacadeService: PermissionManageFacadeService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val userManageService: UserManageService
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

    fun getUserProjectsByAction(
        userId: String,
        action: String
    ): List<String> {
        // 获取用户的所属组织
        val memberDeptInfos = userManageService.getUserInfo(userId).path?.map { it.toString() } ?: emptyList()
        return permissionResourceGroupPermissionService.listProjectsWithPermission(
            memberIds = memberDeptInfos.toMutableList().plus(userId),
            action = action
        )
    }
}

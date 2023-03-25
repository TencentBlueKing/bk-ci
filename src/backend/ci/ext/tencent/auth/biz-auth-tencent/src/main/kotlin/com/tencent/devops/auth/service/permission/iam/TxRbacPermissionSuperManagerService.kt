package com.tencent.devops.auth.service.permission.iam

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.auth.service.PermissionSuperManagerService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.RbacAuthUtils

class TxRbacPermissionSuperManagerService(
    private val managerService: ManagerService
) : PermissionSuperManagerService {
    override fun reviewManagerCheck(
        userId: String,
        projectCode: String,
        resourceType: String,
        action: String
    ): Boolean {
        val projectManageAction = RbacAuthUtils.buildAction(AuthPermission.MANAGE, AuthResourceType.PROJECT)
        if (action == projectManageAction) {
            return false
        }
        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectCode,
            resourceType = RbacAuthUtils.getResourceTypeByStr(resourceType),
            authPermission = RbacAuthUtils.getAuthPermissionByAction(action)
        )
    }
}

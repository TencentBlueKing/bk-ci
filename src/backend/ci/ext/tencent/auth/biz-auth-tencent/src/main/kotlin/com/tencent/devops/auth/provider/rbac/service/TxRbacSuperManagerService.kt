package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.auth.service.SuperManagerService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.rbac.utils.RbacAuthUtils

class TxRbacSuperManagerService(
    private val managerService: ManagerService
) : SuperManagerService {

    override fun projectManagerCheck(
        userId: String,
        projectCode: String,
        action: String,
        resourceType: String
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

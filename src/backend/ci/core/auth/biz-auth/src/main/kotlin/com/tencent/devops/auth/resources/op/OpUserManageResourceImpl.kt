package com.tencent.devops.auth.resources.op

import com.tencent.devops.auth.api.op.OpUserManageResource
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.auth.service.UserManageService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import com.tencent.devops.common.web.RestResource

@RestResource
class OpUserManageResourceImpl(
    private val userManageService: UserManageService,
    private val permissionAuthorizationService: PermissionAuthorizationService
) : OpUserManageResource {
    override fun syncAllUserInfoData(): Result<Boolean> {
        userManageService.syncAllUserInfoData()
        return Result(true)
    }

    override fun syncUserInfoData(userIds: List<String>): Result<Boolean> {
        userManageService.syncUserInfoData(userIds)
        return Result(true)
    }

    override fun syncDepartmentInfoData(): Result<Boolean> {
        userManageService.syncDepartmentInfoData()
        return Result(true)
    }

    override fun syncDepartmentRelations(): Result<Boolean> {
        userManageService.syncDepartmentRelations()
        return Result(true)
    }

    override fun resetResourceAuthorization(
        projectId: String,
        condition: ResourceAuthorizationHandoverConditionRequest
    ): Result<Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>>> {
        return Result(
            permissionAuthorizationService.resetResourceAuthorizationByResourceType(
                operator = "system",
                projectCode = projectId,
                condition = condition
            )
        )
    }
}

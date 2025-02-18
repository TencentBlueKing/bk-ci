package com.tencent.devops.auth.resources.user

import com.tencent.devops.auth.api.user.UserAuthAuthorizationResource
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.vo.AuthProjectVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BkManagerCheck
import com.tencent.devops.common.auth.api.pojo.ResetAllResourceAuthorizationReq
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationResponse
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import com.tencent.devops.common.web.RestResource

@RestResource
class UserAuthAuthorizationResourceImpl(
    val permissionAuthorizationService: PermissionAuthorizationService,
    val permissionResourceValidateService: PermissionResourceValidateService
) : UserAuthAuthorizationResource {
    override fun listResourceAuthorization(
        userId: String,
        projectId: String,
        operateChannel: OperateChannel?,
        condition: ResourceAuthorizationConditionRequest
    ): Result<SQLPage<ResourceAuthorizationResponse>> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = operateChannel ?: OperateChannel.MANAGER,
            targetMemberId = if (operateChannel == OperateChannel.PERSONAL) condition.handoverFrom!! else userId
        )
        return Result(
            permissionAuthorizationService.listResourceAuthorizations(
                condition = condition,
                operateChannel = operateChannel
            )
        )
    }

    override fun getResourceAuthorization(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<ResourceAuthorizationResponse> {
        return Result(
            permissionAuthorizationService.getResourceAuthorization(
                resourceType = resourceType,
                projectCode = projectId,
                resourceCode = resourceCode,
                executePermissionCheck = true
            )
        )
    }

    override fun checkAuthorizationWhenRemoveGroupMember(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        memberId: String
    ): Result<Boolean> {
        return Result(
            permissionAuthorizationService.checkAuthorizationWhenRemoveGroupMember(
                userId = userId,
                projectCode = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                memberId = memberId
            )
        )
    }

    override fun resetResourceAuthorization(
        userId: String,
        projectId: String,
        condition: ResourceAuthorizationHandoverConditionRequest
    ): Result<Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>>> {
        return Result(
            permissionAuthorizationService.resetResourceAuthorizationByResourceType(
                operator = userId,
                projectCode = projectId,
                condition = condition
            )
        )
    }

    @BkManagerCheck
    override fun resetAllResourceAuthorization(
        userId: String,
        projectId: String,
        condition: ResetAllResourceAuthorizationReq
    ): Result<List<ResourceTypeInfoVo>> {
        return Result(
            permissionAuthorizationService.resetAllResourceAuthorization(
                operator = userId,
                projectCode = projectId,
                condition = condition
            )
        )
    }

    override fun listUserProjectsWithAuthorization(userId: String): Result<List<AuthProjectVO>> {
        return Result(permissionAuthorizationService.listUserProjectsWithAuthorization(userId))
    }
}

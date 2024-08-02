package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.user.UserAuthAuthorizationResource
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.PermissionAuthorizationService
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
    val permissionAuthorizationService: PermissionAuthorizationService
) : UserAuthAuthorizationResource {
    @BkManagerCheck
    override fun listResourceAuthorization(
        userId: String,
        projectId: String,
        condition: ResourceAuthorizationConditionRequest
    ): Result<SQLPage<ResourceAuthorizationResponse>> {
        return Result(
            permissionAuthorizationService.listResourceAuthorizations(
                condition = condition
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
                resourceCode = resourceCode
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
}

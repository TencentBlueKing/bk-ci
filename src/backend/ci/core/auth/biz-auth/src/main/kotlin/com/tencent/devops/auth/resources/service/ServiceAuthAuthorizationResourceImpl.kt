package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceAuthAuthorizationResource
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationResponse
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceAuthAuthorizationResourceImpl constructor(
    val permissionAuthorizationService: PermissionAuthorizationService
) : ServiceAuthAuthorizationResource {
    override fun addResourceAuthorization(
        projectId: String,
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ): Result<Boolean> {
        return Result(
            permissionAuthorizationService.addResourceAuthorization(
                resourceAuthorizationList = resourceAuthorizationList
            )
        )
    }

    override fun getResourceAuthorization(
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<ResourceAuthorizationResponse> {
        return Result(
            permissionAuthorizationService.getResourceAuthorization(
                projectCode = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }

    override fun listResourceAuthorization(
        projectId: String,
        condition: ResourceAuthorizationConditionRequest
    ): Result<SQLPage<ResourceAuthorizationResponse>> {
        return Result(
            permissionAuthorizationService.listResourceAuthorizations(
                condition = condition
            )
        )
    }

    override fun batchModifyHandoverFrom(
        projectId: String,
        resourceAuthorizationHandoverList: List<ResourceAuthorizationHandoverDTO>
    ): Result<Boolean> {
        return Result(
            permissionAuthorizationService.batchModifyHandoverFrom(
                resourceAuthorizationHandoverList = resourceAuthorizationHandoverList
            )
        )
    }
}

package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceResourceAuthResource
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.api.pojo.BkAuthResourceGroup
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceResourceAuthResourceImpl constructor(
    private val resourceGroupService: PermissionResourceGroupService
) : ServiceResourceAuthResource {
    override fun getResourceUsers(
        token: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        group: BkAuthResourceGroup?
    ): Result<List<String>> {
        return Result(
            resourceGroupService.getResourceGroupUsers(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                group = group?.value
            )
        )
    }

    override fun getProjectGroupAndUserList(
        token: String,
        projectCode: String,
        resourceType: String
    ): Result<List<BkAuthGroupAndUserList>> {
        return Result(
            resourceGroupService.getResourceGroupAndUserList(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = projectCode
            )
        )
    }
}

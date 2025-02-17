package com.tencent.devops.auth.resources.open

import com.tencent.devops.auth.api.open.OpenResourceMemberResource
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType

@RestResource
class OpenResourceMemberResourceImpl(
    private val permissionResourceMemberService: PermissionResourceMemberService
) : OpenResourceMemberResource {
    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun getResourceGroupMembers(
        token: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        group: BkAuthGroup?
    ): Result<List<String>> {
        return Result(
            permissionResourceMemberService.getResourceGroupMembers(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                group = group
            )
        )
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun getResourceGroupAndMembers(
        token: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Result<List<BkAuthGroupAndUserList>> {
        return Result(
            permissionResourceMemberService.getResourceGroupAndMembers(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }
}

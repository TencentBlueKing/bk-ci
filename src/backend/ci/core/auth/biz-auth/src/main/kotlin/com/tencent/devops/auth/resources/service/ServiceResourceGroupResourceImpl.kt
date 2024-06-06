package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceResourceGroupResource
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceResourceGroupResourceImpl constructor(
    val permissionResourceGroupService: PermissionResourceGroupService,
    val permissionProjectService: PermissionProjectService
) : ServiceResourceGroupResource {
    override fun createGroupByGroupCode(
        userId: String,
        projectCode: String,
        resourceType: String,
        groupCode: BkAuthGroup
    ): Result<Boolean> {
        return Result(
            permissionResourceGroupService.createGroupByGroupCode(
                userId = userId,
                projectId = projectCode,
                groupCode = groupCode.value,
                resourceType = resourceType
            )
        )
    }

    override fun createGroup(
        userId: String,
        projectCode: String,
        groupAddDTO: GroupAddDTO
    ): Result<Int> {
        return Result(
            permissionResourceGroupService.createGroup(
                userId = userId,
                projectId = projectCode,
                groupAddDTO = groupAddDTO
            )
        )
    }

    override fun deleteGroup(
        userId: String,
        projectCode: String,
        resourceType: String,
        groupId: Int
    ): Result<Boolean> {
        return Result(
            permissionResourceGroupService.deleteGroup(
                userId = userId,
                projectId = projectCode,
                groupId = groupId,
                resourceType = resourceType
            )
        )
    }
}

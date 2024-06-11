package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceResourceGroupResource
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceResourceGroupResourceImpl constructor(
    val permissionResourceGroupService: PermissionResourceGroupService
) : ServiceResourceGroupResource {
    override fun getGroupPermissionDetail(
        projectCode: String,
        groupId: Int
    ): Result<Map<String, List<GroupPermissionDetailVo>>> {
        return Result(
            permissionResourceGroupService.getGroupPermissionDetail(
                groupId = groupId
            )
        )
    }

    override fun createGroupByGroupCode(
        projectCode: String,
        resourceType: String,
        groupCode: BkAuthGroup
    ): Result<Boolean> {
        return Result(
            permissionResourceGroupService.createProjectGroupByGroupCode(
                projectId = projectCode,
                groupCode = groupCode.value
            )
        )
    }

    override fun createGroup(
        projectCode: String,
        groupAddDTO: GroupAddDTO
    ): Result<Int> {
        return Result(
            data = permissionResourceGroupService.createGroup(
                projectId = projectCode,
                groupAddDTO = groupAddDTO
            )
        )
    }

    override fun deleteGroup(
        projectCode: String,
        resourceType: String,
        groupId: Int
    ): Result<Boolean> {
        return Result(
            permissionResourceGroupService.deleteGroup(
                userId = null,
                projectId = projectCode,
                groupId = groupId,
                resourceType = resourceType
            )
        )
    }
}

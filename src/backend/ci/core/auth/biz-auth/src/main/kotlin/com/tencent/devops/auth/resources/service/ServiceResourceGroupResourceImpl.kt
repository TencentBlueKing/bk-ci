package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceResourceGroupResource
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceResourceGroupResourceImpl(
    val permissionResourceGroupService: PermissionResourceGroupService,
    val resourceGroupPermissionService: PermissionResourceGroupPermissionService,
    val permissionManageFacadeService: PermissionManageFacadeService
) : ServiceResourceGroupResource {
    override fun getGroupPermissionDetail(
        projectCode: String,
        groupId: Int
    ): Result<Map<String, List<GroupPermissionDetailVo>>> {
        return Result(
            resourceGroupPermissionService.getGroupPermissionDetail(
                iamGroupId = groupId
            )
        )
    }

    override fun getMemberGroupsDetails(
        userId: String,
        projectCode: String,
        resourceType: String,
        memberId: String,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        start: Int?,
        limit: Int?
    ): Result<SQLPage<GroupDetailsInfoVo>> {
        return Result(
            permissionManageFacadeService.getMemberGroupsDetails(
                projectId = projectCode,
                resourceType = resourceType,
                memberId = memberId,
                groupName = groupName,
                minExpiredAt = minExpiredAt,
                maxExpiredAt = maxExpiredAt,
                relatedResourceType = relatedResourceType,
                relatedResourceCode = relatedResourceCode,
                action = action,
                start = start,
                limit = limit
            )
        )
    }

    override fun createGroupByGroupCode(
        projectCode: String,
        resourceType: String,
        groupCode: BkAuthGroup,
        groupName: String?,
        groupDesc: String?
    ): Result<Int> {
        return Result(
            data = permissionResourceGroupService.createGroupAndPermissionsByGroupCode(
                projectId = projectCode,
                resourceType = resourceType,
                resourceCode = projectCode,
                groupCode = groupCode.value,
                groupName = groupName,
                groupDesc = groupDesc
            )
        )
    }

    override fun createCustomGroupAndPermissions(
        projectCode: String,
        customGroupCreateReq: CustomGroupCreateReq
    ): Result<Int> {
        return Result(
            data = permissionResourceGroupService.createCustomGroupAndPermissions(
                projectId = projectCode,
                customGroupCreateReq = customGroupCreateReq
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

    override fun getByGroupCode(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupCode: BkAuthGroup
    ): Result<AuthResourceGroup?> {
        return Result(
            permissionResourceGroupService.getByGroupCode(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                groupCode = groupCode
            )
        )
    }
}

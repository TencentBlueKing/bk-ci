package com.tencent.devops.auth.resources

import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.devops.auth.api.user.UserAuthApplyResource
import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.ApplyJoinProjectInfo
import com.tencent.devops.auth.pojo.SearchGroupInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAuthApplyResourceImpl @Autowired constructor(
    val permissionApplyService: PermissionApplyService
) : UserAuthApplyResource {
    override fun listResourceTypes(userId: String): Result<List<ResourceTypeInfoVo>> {
        return Result(permissionApplyService.listResourceTypes(userId))
    }

    override fun listActions(userId: String, resourceType: String): Result<List<ActionInfoVo>> {
        return Result(permissionApplyService.listActions(userId, resourceType))
    }

    override fun listGroups(
        userId: String,
        projectId: String,
        searchGroupInfo: SearchGroupInfo
    ): Result<V2ManagerRoleGroupVO> {
        return Result(
            permissionApplyService.listGroups(
                userId = userId,
                projectId = projectId,
                searchGroupInfo = searchGroupInfo
            )
        )
    }

    override fun applyToJoinGroup(userId: String, applyJoinGroupInfo: ApplyJoinGroupInfo): Result<Boolean> {
        return Result(permissionApplyService.applyToJoinGroup(userId, applyJoinGroupInfo))
    }

    override fun applyToJoinProject(
        userId: String,
        projectId: String,
        applyJoinProjectInfo: ApplyJoinProjectInfo
    ): Result<Boolean> {
        return Result(
            permissionApplyService.applyToJoinProject(
                userId = userId,
                projectId = projectId,
                applyJoinProjectInfo = applyJoinProjectInfo
            )
        )
    }

    override fun getGroupPermissionDetail(userId: String, groupId: Int): Result<List<GroupPermissionDetailVo>> {
        return Result(permissionApplyService.getGroupPermissionDetail(userId, groupId))
    }

    override fun getRedirectInformation(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        action: String
    ): Result<AuthApplyRedirectInfoVo> {
        return Result(
            permissionApplyService.getRedirectInformation(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action
            )
        )
    }
}

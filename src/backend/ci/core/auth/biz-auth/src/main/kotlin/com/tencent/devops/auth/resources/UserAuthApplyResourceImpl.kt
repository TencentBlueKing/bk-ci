package com.tencent.devops.auth.resources

import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.devops.auth.api.user.UserAuthApplyResource
import com.tencent.devops.auth.pojo.ApplicationInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.web.RestResource

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

    override fun listGroups(userId: String, projectId: String, inherit: Boolean?, actionId: String?, resourceType: String?, resourceCode: String?, bkIamPath: String?, name: String?, description: String?, page: Int, pageSize: Int): Result<V2ManagerRoleGroupVO> {
        return Result(
            permissionApplyService.listGroups(
                userId = userId,
                projectId = projectId,
                inherit = inherit,
                actionId = actionId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                bkIamPath = bkIamPath,
                name = name,
                description = description,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun applyToJoinGroup(userId: String, applicationInfo: ApplicationInfo): Result<Boolean> {
        return Result(permissionApplyService.applyToJoinGroup(userId, applicationInfo))
    }

    override fun getGroupPermissionDetail(userId: String, groupId: Int): Result<List<GroupPermissionDetailVo>> {
        return Result(permissionApplyService.getGroupPermissionDetail(userId, groupId))
    }
}

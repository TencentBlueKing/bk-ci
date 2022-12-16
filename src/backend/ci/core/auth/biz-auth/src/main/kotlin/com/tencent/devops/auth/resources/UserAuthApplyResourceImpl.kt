package com.tencent.devops.auth.resources

import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.devops.auth.api.user.UserAuthApplyResource
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class UserAuthApplyResourceImpl @Autowired constructor(
    val PermissionApplyService: PermissionApplyService
) : UserAuthApplyResource {
    override fun listResourceTypes(userId: String): Result<List<ResourceTypeInfoVo>> {
        return Result(PermissionApplyService.listResourceTypes(userId))
    }

    override fun listActions(userId: String, resourceType: String): Result<List<ActionInfoVo>> {
        return Result(PermissionApplyService.listActions(userId, resourceType))
    }

    override fun listGroups(userId: String, projectId: String, inherit: Boolean?, actionId: String?, resourceType: String?, resourceCode: String?, bkIamPath: String?, name: String?, description: String?, page: Int, pageSize: Int): Result<V2ManagerRoleGroupVO> {
        return Result(
            PermissionApplyService.listGroups(
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

    override fun applyToJoinGroup(userId: String, applicationDTO: ApplicationDTO): Result<Boolean> {
        return Result(PermissionApplyService.applyToJoinGroup(userId, applicationDTO))
    }
}

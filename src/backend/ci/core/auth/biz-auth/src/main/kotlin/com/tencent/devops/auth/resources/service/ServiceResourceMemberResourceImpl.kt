package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import java.util.concurrent.TimeUnit

@RestResource
class ServiceResourceMemberResourceImpl constructor(
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val permissionProjectService: PermissionProjectService
) : ServiceResourceMemberResource {
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

    override fun batchAddResourceGroupMembers(
        token: String,
        userId: String,
        projectCode: String,
        projectCreateUserInfo: ProjectCreateUserInfo
    ): Result<Boolean> {
        val isProjectManager = permissionProjectService.checkProjectManager(
            userId = userId,
            projectCode = projectCode
        )
        if (!isProjectManager) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.NOT_MANAGER,
                defaultMessage = "The user($userId) is not the manager of the project($projectCode)!"
            )
        }
        with(projectCreateUserInfo) {
            val iamGroupId = groupId ?: permissionResourceMemberService.roleCodeToIamGroupId(
                projectCode = projectCode,
                roleCode = (if (roleName != null) BkAuthGroup.roleNameToRoleId(roleName!!) else roleId!!).toString()
            )
            val expiredTime = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(365L)
            return Result(
                permissionResourceMemberService.batchAddResourceGroupMembers(
                    userId = userId,
                    projectCode = projectCode,
                    iamGroupId = iamGroupId,
                    expiredTime = expiredTime,
                    members = userIds,
                    departments = deptIds
                )
            )
        }
    }
}

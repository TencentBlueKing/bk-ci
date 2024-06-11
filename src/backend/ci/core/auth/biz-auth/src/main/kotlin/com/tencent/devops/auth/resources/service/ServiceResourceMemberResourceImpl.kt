package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectDeleteUserInfo
import java.util.concurrent.TimeUnit

@RestResource
class ServiceResourceMemberResourceImpl constructor(
    private val permissionResourceMemberService: PermissionResourceMemberService
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
        projectCode: String,
        projectCreateUserInfo: ProjectCreateUserInfo
    ): Result<Boolean> {
        with(projectCreateUserInfo) {
            val expiredTime = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(365L)
            return Result(
                permissionResourceMemberService.batchAddResourceGroupMembers(
                    projectCode = projectCode,
                    iamGroupId = getIamGroupId(
                        groupId = groupId,
                        projectCode = projectCode,
                        roleName = roleName,
                        roleId = roleId
                    ),
                    expiredTime = expiredTime,
                    members = userIds,
                    departments = deptIds
                )
            )
        }
    }

    override fun batchDeleteResourceGroupMembers(
        token: String,
        projectCode: String,
        projectDeleteUserInfo: ProjectDeleteUserInfo
    ): Result<Boolean> {
        with(projectDeleteUserInfo) {
            return Result(
                permissionResourceMemberService.batchDeleteResourceGroupMembers(
                    projectCode = projectCode,
                    iamGroupId = getIamGroupId(
                        groupId = groupId,
                        projectCode = projectCode,
                        roleName = roleName,
                        roleId = roleId
                    ),
                    members = userIds,
                    departments = deptIds
                )
            )
        }
    }

    private fun getIamGroupId(
        groupId: Int?,
        projectCode: String,
        roleName: String?,
        roleId: Int?
    ): Int {
        return groupId ?: permissionResourceMemberService.roleCodeToIamGroupId(
            projectCode = projectCode,
            roleCode = roleName ?: BkAuthGroup.getByRoleId(roleId!!).value
        )
    }
}

package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectDeleteUserInfo
import java.util.concurrent.TimeUnit

@RestResource
class ServiceResourceMemberResourceImpl(
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val permissionManageFacadeService: PermissionManageFacadeService
) : ServiceResourceMemberResource {
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

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun batchAddResourceGroupMembers(
        token: String,
        projectCode: String,
        projectCreateUserInfo: ProjectCreateUserInfo
    ): Result<Boolean> {
        with(projectCreateUserInfo) {
            val now = System.currentTimeMillis() / 1000
            val fixExpiredTime = now + TimeUnit.DAYS.toSeconds(expiredTime ?: 365L)
            return Result(
                permissionResourceMemberService.batchAddResourceGroupMembers(
                    projectCode = projectCode,
                    iamGroupId = getIamGroupId(
                        groupId = groupId,
                        projectCode = projectCode,
                        roleName = roleName,
                        roleId = roleId,
                        resourceCode = resourceCode ?: projectCode,
                        resourceType = resourceType ?: AuthResourceType.PROJECT.value
                    ),
                    expiredTime = fixExpiredTime,
                    members = userIds,
                    departments = deptIds
                )
            )
        }
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
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
                        roleId = roleId,
                        resourceType = resourceType ?: AuthResourceType.PROJECT.value,
                        resourceCode = resourceCode ?: projectCode
                    ),
                    members = userIds,
                    departments = deptIds
                )
            )
        }
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun renewalGroupMember(
        token: String,
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberSingleRenewalReq
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.renewalGroupMember(
                userId = userId,
                projectCode = projectCode,
                renewalConditionReq = renewalConditionReq
            )
        )
    }

    override fun listProjectMembers(
        projectCode: String,
        memberType: String?,
        userName: String?,
        deptName: String?,
        departedFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<ResourceMemberInfo>> {
        return Result(
            permissionResourceMemberService.listProjectMembers(
                projectCode = projectCode,
                memberType = memberType,
                userName = userName,
                deptName = deptName,
                departedFlag = departedFlag ?: false,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getMemberGroupCount(
        projectCode: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?
    ): Result<List<ResourceType2CountVo>> {
        return Result(
            permissionManageFacadeService.getMemberGroupsCount(
                projectCode = projectCode,
                memberId = memberId,
                groupName = null,
                minExpiredAt = null,
                maxExpiredAt = null,
                relatedResourceType = relatedResourceType,
                relatedResourceCode = relatedResourceCode,
                action = null,
                operateChannel = null,
                uniqueManagerGroupsQueryFlag = null
            )
        )
    }

    override fun removeMemberFromProject(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>> {
        return Result(
            permissionManageFacadeService.removeMemberFromProject(
                userId = userId,
                projectCode = projectCode,
                removeMemberFromProjectReq = removeMemberFromProjectReq
            )
        )
    }

    override fun removeMemberFromProjectCheck(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.removeMemberFromProjectCheck(
                userId = userId,
                projectCode = projectCode,
                removeMemberFromProjectReq = removeMemberFromProjectReq
            )
        )
    }

    override fun batchRenewalGroupMembersFromManager(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.batchRenewalGroupMembersFromManager(
                userId = userId,
                projectCode = projectCode,
                renewalConditionReq = renewalConditionReq
            )
        )
    }

    override fun batchRemoveGroupMembersFromManager(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService
                .batchDeleteResourceGroupMembersFromManager(
                    userId = userId,
                    projectCode = projectCode,
                    removeMemberDTO = removeMemberDTO
                )
        )
    }

    override fun batchOperateGroupMembersCheck(
        userId: String,
        projectCode: String,
        batchOperateType: BatchOperateType,
        conditionReq: GroupMemberCommonConditionReq
    ): Result<BatchOperateGroupMemberCheckVo> {
        return Result(
            permissionManageFacadeService.batchOperateGroupMembersCheck(
                userId = userId,
                projectCode = projectCode,
                batchOperateType = batchOperateType,
                conditionReq = conditionReq
            )
        )
    }

    override fun batchHandoverGroupMembersFromManager(
        userId: String,
        projectCode: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.batchHandoverGroupMembersFromManager(
                userId = userId,
                projectCode = projectCode,
                handoverMemberDTO = handoverMemberDTO
            )
        )
    }

    private fun getIamGroupId(
        groupId: Int?,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        roleName: String?,
        roleId: Int?
    ): Int {
        return groupId ?: permissionResourceMemberService.roleCodeToIamGroupId(
            projectCode = projectCode,
            roleCode = roleName ?: BkAuthGroup.getByRoleId(roleId!!).value,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }
}

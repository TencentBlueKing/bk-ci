package com.tencent.devops.auth.resources.user

import com.tencent.devops.auth.api.user.UserAuthResourceMemberResource
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectResponse
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberExitsProjectCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BkManagerCheck
import com.tencent.devops.common.auth.rbac.utils.RbacAuthUtils
import com.tencent.devops.common.web.RestResource

@RestResource
class UserAuthResourceMemberResourceImpl(
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val permissionService: PermissionService,
    private val permissionManageFacadeService: PermissionManageFacadeService,
    private val permissionResourceValidateService: PermissionResourceValidateService
) : UserAuthResourceMemberResource {
    override fun listProjectMembers(
        userId: String,
        projectId: String,
        memberType: String?,
        userName: String?,
        deptName: String?,
        departedFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<ResourceMemberInfo>> {
        val hasVisitPermission = permissionService.validateUserResourcePermission(
            userId = userId,
            resourceType = AuthResourceType.PROJECT.value,
            action = RbacAuthUtils.buildAction(AuthPermission.VISIT, AuthResourceType.PROJECT),
            projectCode = projectId
        )
        return if (!hasVisitPermission) {
            Result(SQLPage(0, emptyList()))
        } else {
            Result(
                permissionResourceMemberService.listProjectMembers(
                    projectCode = projectId,
                    memberType = memberType,
                    userName = userName,
                    deptName = deptName,
                    departedFlag = departedFlag ?: false,
                    page = page,
                    pageSize = pageSize
                )
            )
        }
    }

    @BkManagerCheck
    override fun listProjectMembersByCondition(
        userId: String,
        projectId: String,
        projectMembersQueryConditionReq: ProjectMembersQueryConditionReq
    ): Result<SQLPage<ResourceMemberInfo>> {
        return Result(
            permissionManageFacadeService.listProjectMembersByComplexConditions(
                conditionReq = projectMembersQueryConditionReq
            )
        )
    }

    @BkManagerCheck
    override fun renewalGroupMember(
        userId: String,
        projectId: String,
        renewalConditionReq: GroupMemberSingleRenewalReq
    ): Result<GroupDetailsInfoVo> {
        permissionManageFacadeService.renewalGroupMember(
            userId = userId,
            projectCode = projectId,
            renewalConditionReq = renewalConditionReq
        )
        return Result(
            permissionManageFacadeService.getMemberGroupsDetails(
                projectId = projectId,
                memberId = renewalConditionReq.targetMember.id,
                iamGroupIds = listOf(renewalConditionReq.groupId)
            ).records.first { it.groupId == renewalConditionReq.groupId }
        )
    }

    @BkManagerCheck
    override fun batchRenewalGroupMembersFromManager(
        userId: String,
        projectId: String,
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.batchRenewalGroupMembersFromManager(
                userId = userId,
                projectCode = projectId,
                renewalConditionReq = renewalConditionReq
            )
        )
    }

    @BkManagerCheck
    override fun batchRemoveGroupMembersFromManager(
        userId: String,
        projectId: String,
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.batchDeleteResourceGroupMembersFromManager(
                userId = userId,
                projectCode = projectId,
                removeMemberDTO = removeMemberDTO
            )
        )
    }

    override fun batchRemoveGroupMembersFromPersonal(
        userId: String,
        projectId: String,
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): Result<String> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = OperateChannel.PERSONAL,
            targetMemberId = removeMemberDTO.targetMember.id
        )
        return Result(
            permissionManageFacadeService.batchDeleteResourceGroupMembersFromPersonal(
                userId = userId,
                projectCode = projectId,
                removeMemberDTO = removeMemberDTO
            )
        )
    }

    override fun deleteResourceGroupMembers(
        userId: String,
        projectId: String,
        groupId: Int,
        operateChannel: OperateChannel,
        targetMember: ResourceMemberInfo
    ): Result<Boolean> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = operateChannel,
            targetMemberId = targetMember.id
        )
        return Result(
            permissionManageFacadeService.deleteResourceGroupMembers(
                userId = userId,
                projectCode = projectId,
                groupId = groupId,
                targetMember = targetMember
            )
        )
    }

    @BkManagerCheck
    override fun batchHandoverGroupMembersFromManager(
        userId: String,
        projectId: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.batchHandoverGroupMembersFromManager(
                userId = userId,
                projectCode = projectId,
                handoverMemberDTO = handoverMemberDTO
            )
        )
    }

    override fun batchHandoverApplicationFromPersonal(
        userId: String,
        projectId: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Result<String> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = OperateChannel.PERSONAL,
            targetMemberId = handoverMemberDTO.targetMember.id
        )
        return Result(
            permissionManageFacadeService.batchHandoverApplicationFromPersonal(
                userId = userId,
                projectCode = projectId,
                handoverMemberDTO = handoverMemberDTO
            )
        )
    }

    override fun batchOperateGroupMembersCheck(
        userId: String,
        projectId: String,
        batchOperateType: BatchOperateType,
        conditionReq: GroupMemberCommonConditionReq
    ): Result<BatchOperateGroupMemberCheckVo> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = conditionReq.operateChannel,
            targetMemberId = conditionReq.targetMember.id
        )
        return Result(
            permissionManageFacadeService.batchOperateGroupMembersCheck(
                userId = userId,
                projectCode = projectId,
                batchOperateType = batchOperateType,
                conditionReq = conditionReq
            )
        )
    }

    @BkManagerCheck
    override fun removeMemberFromProject(
        userId: String,
        projectId: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>> {
        return Result(
            permissionManageFacadeService.removeMemberFromProject(
                userId = userId,
                projectCode = projectId,
                removeMemberFromProjectReq = removeMemberFromProjectReq
            )
        )
    }

    @BkManagerCheck
    override fun batchRemoveMemberFromProject(
        userId: String,
        projectId: String,
        removeMemberFromProjectReq: BatchRemoveMemberFromProjectReq
    ): Result<BatchRemoveMemberFromProjectResponse> {
        return Result(
            permissionManageFacadeService.batchRemoveMemberFromProject(
                userId = userId,
                projectCode = projectId,
                removeMemberFromProjectReq = removeMemberFromProjectReq
            )
        )
    }

    @BkManagerCheck
    override fun removeMemberFromProjectCheck(
        userId: String,
        projectId: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.removeMemberFromProjectCheck(
                userId = userId,
                projectCode = projectId,
                removeMemberFromProjectReq = removeMemberFromProjectReq
            )
        )
    }

    @BkManagerCheck
    override fun batchRemoveMemberFromProjectCheck(
        userId: String,
        projectId: String,
        targetMembers: List<ResourceMemberInfo>
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.batchRemoveMemberFromProjectCheck(
                userId = userId,
                projectCode = projectId,
                targetMembers = targetMembers
            )
        )
    }

    override fun getMemberGroupCount(
        userId: String,
        projectId: String,
        memberId: String,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        operateChannel: OperateChannel?,
        uniqueManagerGroupsQueryFlag: Boolean?,
    ): Result<List<ResourceType2CountVo>> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = operateChannel ?: OperateChannel.MANAGER,
            targetMemberId = memberId
        )
        return Result(
            permissionManageFacadeService.getMemberGroupsCount(
                projectCode = projectId,
                memberId = memberId,
                groupName = groupName,
                minExpiredAt = minExpiredAt,
                maxExpiredAt = maxExpiredAt,
                relatedResourceType = relatedResourceType,
                relatedResourceCode = relatedResourceCode,
                action = action,
                operateChannel = operateChannel,
                uniqueManagerGroupsQueryFlag = uniqueManagerGroupsQueryFlag
            )
        )
    }

    override fun checkMemberExitsProject(
        userId: String,
        projectId: String
    ): Result<MemberExitsProjectCheckVo> {
        return Result(
            permissionManageFacadeService.checkMemberExitsProject(
                projectCode = projectId,
                userId = userId
            )
        )
    }

    override fun memberExitsProject(
        userId: String,
        projectId: String,
        request: RemoveMemberFromProjectReq
    ): Result<String> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = OperateChannel.PERSONAL,
            targetMemberId = request.targetMember.id
        )
        return Result(
            permissionManageFacadeService.memberExitsProject(
                projectCode = projectId,
                request = request
            )
        )
    }
}

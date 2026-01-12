package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.DepartmentUserCount
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.InvalidAuthorizationsDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectResponse
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.HandoverDetailsQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewBatchUpdateReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewUpdateReq
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ResourceType2CountOfHandoverQuery
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.HandoverAuthorizationDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverGroupDetailVo
import com.tencent.devops.auth.pojo.vo.MemberExitsProjectCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.common.api.model.SQLPage

class SamplePermissionManageFacadeService : PermissionManageFacadeService {
    override fun getMemberGroupsDetails(
        projectId: String,
        memberId: String,
        resourceType: String?,
        iamGroupIds: List<Int>?,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        operateChannel: OperateChannel?,
        uniqueManagerGroupsQueryFlag: Boolean?,
        start: Int?,
        limit: Int?
    ): SQLPage<GroupDetailsInfoVo> = SQLPage(0, emptyList())

    override fun getMemberGroupsCount(
        projectCode: String,
        memberId: String,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        operateChannel: OperateChannel?,
        uniqueManagerGroupsQueryFlag: Boolean?
    ): List<ResourceType2CountVo> = emptyList()

    override fun listIamGroupIdsByConditions(
        condition: IamGroupIdsQueryConditionDTO
    ): List<Int> = emptyList()

    override fun listResourceGroupMembers(
        projectCode: String,
        memberId: String,
        resourceType: String?,
        iamGroupIds: List<Int>?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        operateChannel: OperateChannel?,
        filterMemberType: MemberType?,
        excludeIamGroupIds: List<Int>?,
        onlyExcludeUserDirectlyJoined: Boolean?,
        start: Int?,
        limit: Int?
    ): Pair<Long, List<AuthResourceGroupMember>> = Pair(0, emptyList())

    override fun listProjectMembersByComplexConditions(
        conditionReq: ProjectMembersQueryConditionReq
    ): SQLPage<ResourceMemberInfo> = SQLPage(0, emptyList())

    override fun listInvalidAuthorizationsAfterOperatedGroups(
        projectCode: String,
        iamGroupIdsOfDirectlyJoined: List<Int>,
        memberId: String
    ): InvalidAuthorizationsDTO = InvalidAuthorizationsDTO(emptyList(), emptyList())

    override fun renewalGroupMember(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberSingleRenewalReq
    ): Boolean = true

    override fun batchRenewalGroupMembersFromManager(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Boolean = true

    override fun batchHandoverGroupMembersFromManager(
        userId: String,
        projectCode: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Boolean = true

    override fun batchHandoverApplicationFromPersonal(
        userId: String,
        projectCode: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): String = ""

    override fun batchDeleteResourceGroupMembersFromManager(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): Boolean = true

    override fun batchDeleteResourceGroupMembersFromPersonal(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): String = "true"

    override fun deleteResourceGroupMembers(
        userId: String,
        projectCode: String,
        groupId: Int,
        targetMember: ResourceMemberInfo
    ): Boolean = true

    override fun batchOperateGroupMembersCheck(
        userId: String,
        projectCode: String,
        batchOperateType: BatchOperateType,
        conditionReq: GroupMemberCommonConditionReq
    ): BatchOperateGroupMemberCheckVo = BatchOperateGroupMemberCheckVo(totalCount = 0)

    override fun removeMemberFromProject(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): List<ResourceMemberInfo> = emptyList()

    override fun batchRemoveMemberFromProject(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: BatchRemoveMemberFromProjectReq
    ): BatchRemoveMemberFromProjectResponse = BatchRemoveMemberFromProjectResponse(
        users = emptyList(),
        departments = emptyList()
    )

    override fun removeMemberFromProjectCheck(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Boolean = true

    override fun batchRemoveMemberFromProjectCheck(
        userId: String,
        projectCode: String,
        targetMembers: List<ResourceMemberInfo>
    ): Boolean = true

    override fun handleHanoverApplication(request: HandoverOverviewUpdateReq): Boolean = true

    override fun batchHandleHanoverApplications(request: HandoverOverviewBatchUpdateReq): Boolean = true

    override fun getResourceType2CountOfHandover(
        queryReq: ResourceType2CountOfHandoverQuery
    ): List<ResourceType2CountVo> {
        return emptyList()
    }

    override fun listAuthorizationsOfHandover(
        queryReq: HandoverDetailsQueryReq
    ): SQLPage<HandoverAuthorizationDetailVo> {
        return SQLPage(0, emptyList())
    }

    override fun listGroupsOfHandover(queryReq: HandoverDetailsQueryReq): SQLPage<HandoverGroupDetailVo> {
        return SQLPage(0, emptyList())
    }

    override fun isProjectMember(projectCode: String, userId: String): Boolean {
        return true
    }

    override fun checkMemberExitsProject(
        projectCode: String,
        userId: String
    ): MemberExitsProjectCheckVo {
        return MemberExitsProjectCheckVo()
    }

    override fun memberExitsProject(
        projectCode: String,
        request: RemoveMemberFromProjectReq
    ): String {
        return ""
    }

    override fun getProjectUserDepartmentDistribution(
        projectCode: String,
        parentDepartmentId: Int
    ): List<DepartmentUserCount> {
        return emptyList()
    }
}

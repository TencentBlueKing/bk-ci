package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberGroupCountWithPermissionsVo
import com.tencent.devops.common.api.model.SQLPage

interface PermissionResourceGroupAndMemberFacadeService {
    /**
     * 查询成员所在资源用户组详情
     * 管理员视角返回用户直接加入/模板加入的用户组
     * 个人视角返回用户直接加入/模板加入/组织加入的用户组
     * */
    fun getMemberGroupsDetails(
        projectId: String,
        memberId: String,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        groupName: String? = null,
        minExpiredAt: Long? = null,
        maxExpiredAt: Long? = null,
        relatedResourceType: String? = null,
        relatedResourceCode: String? = null,
        action: String? = null,
        operateChannel: OperateChannel? = OperateChannel.MANAGER,
        start: Int? = null,
        limit: Int? = null
    ): SQLPage<GroupDetailsInfoVo>

    /**
     * 获取用户有权限的用户组数量
     * 管理员视角返回用户直接加入/模板加入的用户组
     * 个人视角返回用户直接加入/模板加入/组织加入的用户组
     * */
    fun getMemberGroupsCount(
        projectCode: String,
        memberId: String,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        operateChannel: OperateChannel? = OperateChannel.MANAGER
    ): List<MemberGroupCountWithPermissionsVo>

    /**
     * 根据条件查询组ID
     * */
    fun listIamGroupIdsByConditions(
        condition: IamGroupIdsQueryConditionDTO
    ): List<Int>

    /**
     * 获取用户在该项目加入的组
     * */
    fun listMemberGroupIdsInProject(
        projectCode: String,
        memberId: String
    ): List<Int>

    /**
     * 查询成员所在资源用户组列表
     * 管理员视角返回用户直接加入/模板加入的用户组
     * 个人视角返回用户直接加入/模板加入/组织加入的用户组
     * */
    fun listResourceGroupMembers(
        projectCode: String,
        memberId: String,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        excludeIamGroupIds: List<Int>? = null,
        minExpiredAt: Long? = null,
        maxExpiredAt: Long? = null,
        operateChannel: OperateChannel? = OperateChannel.MANAGER,
        start: Int? = null,
        limit: Int? = null
    ): Pair<Long, List<AuthResourceGroupMember>>

    /**
     * 根据复杂条件进行搜索，用于用户管理界面
     * */
    fun listProjectMembersByComplexConditions(
        conditionReq: ProjectMembersQueryConditionReq
    ): SQLPage<ResourceMemberInfo>

    /**
     * 为了避免流水线代持人权限失效，需要对用户退出/交接用户组进行检查。
     * 返回结果：
     * 1、引起代持人权限失效的用户组。
     * 2、引起代持人权限失效的流水线。
     **/
    fun listInvalidAuthorizationsAfterOperatedGroups(
        projectCode: String,
        iamGroupIds: List<Int>,
        memberId: String
    ): Pair<List<Int>/*引起代持人权限失效的用户组*/, List<String>/*引起代持人权限失效的流水线*/>

    // 无需审批版本
    fun renewalGroupMember(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberSingleRenewalReq
    ): Boolean

    fun batchRenewalGroupMembersFromManager(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Boolean

    fun batchHandoverGroupMembersFromManager(
        userId: String,
        projectCode: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Boolean

    fun batchDeleteResourceGroupMembersFromManager(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberCommonConditionReq
    ): Boolean

    fun batchOperateGroupMembersCheck(
        userId: String,
        projectCode: String,
        batchOperateType: BatchOperateType,
        conditionReq: GroupMemberCommonConditionReq
    ): BatchOperateGroupMemberCheckVo

    fun removeMemberFromProject(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): List<ResourceMemberInfo>

    fun removeMemberFromProjectCheck(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Boolean
}

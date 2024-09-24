package com.tencent.devops.auth.service.iam

import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceMemberCountVO
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList

@Suppress("LongParameterList", "TooManyFunctions")
interface PermissionResourceMemberService {
    fun getResourceGroupMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        group: BkAuthGroup?
    ): List<String>

    fun getResourceGroupAndMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): List<BkAuthGroupAndUserList>

    fun getProjectMemberCount(projectCode: String): ResourceMemberCountVO

    /**
     *  之所以将简单查询接口抽成单独方法，是因为该方法只用于查询用户名称/部门名称等，
    一方面，该方法职责比较单一；另一方面，该接口需要连表查询到授权资源表中授权人。
    复杂查询虽然需要查询各种权限，但是不需要关联授权资源表。
     * */
    fun listProjectMembers(
        projectCode: String,
        memberType: String?,
        userName: String?,
        deptName: String?,
        departedFlag: Boolean? = false,
        page: Int,
        pageSize: Int
    ): SQLPage<ResourceMemberInfo>

    fun addDepartedFlagToMembers(records: List<ResourceMemberInfo>): List<ResourceMemberInfo>

    fun listResourceGroupMembers(
        projectCode: String,
        memberId: String,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        minExpiredAt: Long? = null,
        maxExpiredAt: Long? = null,
        start: Int? = null,
        limit: Int? = null
    ): Pair<Long, List<AuthResourceGroupMember>>

    fun batchDeleteResourceGroupMembers(
        projectCode: String,
        iamGroupId: Int,
        members: List<String>? = emptyList(),
        departments: List<String>? = emptyList()
    ): Boolean

    fun batchDeleteResourceGroupMembers(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberCommonConditionReq
    ): Boolean

    fun deleteIamGroupMembers(
        groupId: Int,
        type: String,
        memberIds: List<String>
    ): Boolean

    fun batchHandoverGroupMembers(
        userId: String,
        projectCode: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
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

    fun roleCodeToIamGroupId(
        projectCode: String,
        roleCode: String
    ): Int

    fun autoRenewal(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        validExpiredDay: Int
    )

    // 需审批版本
    fun renewalGroupMember(
        userId: String,
        projectCode: String,
        resourceType: String,
        groupId: Int,
        memberRenewalDTO: GroupMemberRenewalDTO
    ): Boolean

    // 无需审批版本
    fun renewalGroupMember(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberSingleRenewalReq
    ): Boolean

    fun renewalIamGroupMembers(
        groupId: Int,
        members: List<ManagerMember>,
        expiredAt: Long
    ): Boolean

    fun batchRenewalGroupMembers(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Boolean

    fun addGroupMember(
        projectCode: String,
        memberId: String,
        /*user or department or template*/
        memberType: String,
        expiredAt: Long,
        iamGroupId: Int
    ): Boolean

    fun addIamGroupMember(
        groupId: Int,
        members: List<ManagerMember>,
        expiredAt: Long
    ): Boolean

    fun batchAddResourceGroupMembers(
        projectCode: String,
        iamGroupId: Int,
        expiredTime: Long,
        members: List<String>? = emptyList(),
        departments: List<String>? = emptyList()
    ): Boolean
}

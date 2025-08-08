package com.tencent.devops.auth.provider.sample.service

import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.pojo.vo.ResourceMemberCountVO
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList

class SamplePermissionResourceMemberService : PermissionResourceMemberService {
    override fun getResourceGroupMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        group: BkAuthGroup?
    ): List<String> {
        return emptyList()
    }

    override fun getResourceGroupAndMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): List<BkAuthGroupAndUserList> {
        return emptyList()
    }

    override fun batchAddResourceGroupMembers(
        projectCode: String,
        iamGroupId: Int,
        expiredTime: Long,
        members: List<String>?,
        departments: List<String>?
    ) = true

    override fun batchDeleteResourceGroupMembers(
        projectCode: String,
        iamGroupId: Int,
        members: List<String>?,
        departments: List<String>?
    ): Boolean = true

    override fun roleCodeToIamGroupId(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        roleCode: String
    ): Int = 0

    override fun autoRenewal(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        validExpiredDay: Int
    ) = Unit

    override fun renewalGroupMember(
        userId: String,
        projectCode: String,
        resourceType: String,
        groupId: Int,
        memberRenewalDTO: GroupMemberRenewalDTO
    ): Boolean = true

    override fun renewalIamGroupMembers(
        groupId: Int,
        members: List<ManagerMember>,
        expiredAt: Long
    ): Boolean = true

    override fun deleteIamGroupMembers(
        groupId: Int,
        type: String,
        memberIds: List<String>
    ): Boolean = true

    override fun addGroupMember(
        projectCode: String,
        memberId: String,
        memberType: String,
        expiredAt: Long,
        iamGroupId: Int
    ): Boolean = true

    override fun addIamGroupMember(
        groupId: Int,
        members: List<ManagerMember>,
        expiredAt: Long
    ): Boolean = true

    override fun getProjectMemberCount(projectCode: String): ResourceMemberCountVO =
        ResourceMemberCountVO(
            userCount = 0,
            departmentCount = 0
        )

    override fun listProjectMembers(
        projectCode: String,
        memberType: String?,
        userName: String?,
        deptName: String?,
        departedFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): SQLPage<ResourceMemberInfo> {
        return SQLPage(count = 0, records = emptyList())
    }

    override fun addDepartedFlagToMembers(
        records: List<ResourceMemberInfo>
    ): List<ResourceMemberInfo> = emptyList()
}

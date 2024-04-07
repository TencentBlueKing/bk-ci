package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
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
        userId: String,
        projectCode: String,
        iamGroupId: Int,
        expiredTime: Long,
        members: List<String>?,
        departments: List<String>?
    ) = true

    override fun batchDeleteResourceGroupMembers(
        userId: String,
        projectCode: String,
        iamGroupId: Int,
        members: List<String>?,
        departments: List<String>?
    ): Boolean = true

    override fun roleCodeToIamGroupId(
        projectCode: String,
        roleCode: String
    ): Int = 0

    override fun autoRenewal(projectCode: String, resourceType: String, resourceCode: String) = Unit
}

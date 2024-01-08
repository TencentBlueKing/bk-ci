package com.tencent.devops.auth.service.iam

import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList

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

    fun batchAddResourceGroupMembers(
        userId: String,
        projectCode: String,
        iamGroupId: Int,
        expiredTime: Long,
        members: List<String>
    ): Boolean
}

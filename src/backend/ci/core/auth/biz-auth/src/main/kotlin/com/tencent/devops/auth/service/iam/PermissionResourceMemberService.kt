package com.tencent.devops.auth.service.iam

import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList

interface PermissionResourceMemberService {
    fun getResourceGroupMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        group: String?
    ): List<String>

    fun getResourceGroupAndMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): List<BkAuthGroupAndUserList>
}

package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.SearchGroupInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.ManagerRoleGroupVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo

interface PermissionApplyService {
    fun listResourceTypes(userId: String): List<ResourceTypeInfoVo>

    fun listActions(
        userId: String,
        resourceType: String
    ): List<ActionInfoVo>

    fun listGroups(
        userId: String,
        projectId: String,
        searchGroupInfo: SearchGroupInfo
    ): ManagerRoleGroupVO

    fun applyToJoinGroup(
        userId: String,
        applyJoinGroupInfo: ApplyJoinGroupInfo
    ): Boolean

    fun getGroupPermissionDetail(
        userId: String,
        groupId: Int
    ): List<GroupPermissionDetailVo>

    fun getRedirectInformation(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        action: String?
    ): AuthApplyRedirectInfoVo
}

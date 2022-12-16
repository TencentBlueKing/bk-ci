package com.tencent.devops.auth.service.iam

import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.pojo.Result

interface PermissionApplyService {
    fun listResourceTypes(userId: String): List<ResourceTypeInfoVo>

    fun listActions(
        userId: String,
        resourceType: String
    ): List<ActionInfoVo>

    fun listGroups(
        userId: String,
        projectId: String,
        inherit: Boolean?,
        actionId: String?,
        resourceType: String?,
        resourceCode: String?,
        bkIamPath: String?,
        name: String?,
        description: String?,
        page: Int,
        pageSize: Int
    ): V2ManagerRoleGroupVO

    fun applyToJoinGroup(
        userId: String,
        applicationDTO: ApplicationDTO
    ): Boolean
}

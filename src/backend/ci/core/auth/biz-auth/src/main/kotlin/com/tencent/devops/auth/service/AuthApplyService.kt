package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class AuthApplyService @Autowired constructor(

) {
    fun listResourceTypes(userId: String): Result<List<ResourceTypeInfoVo>> {
        TODO("Not yet implemented")
    }

    fun listActions(userId: String, resourceType: String?): Result<List<ActionInfoVo>> {
        TODO("Not yet implemented")
    }

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
    ): Result<V2ManagerRoleGroupVO> {
        TODO("Not yet implemented")
    }

    fun applyToJoinGroup(userId: String, projectId: String, applicationDTO: ApplicationDTO): Result<Boolean> {
        TODO("Not yet implemented")
    }
}

package com.tencent.devops.experience.resources.app

import com.tencent.devops.auth.api.service.ServiceAuthApplyResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperiencePermissionResource

@RestResource
class AppExperiencePermissionResourceImpl(
    val client: Client,
    val tokenService: ClientTokenService,
) : AppExperiencePermissionResource {
    override fun applyToJoinGroup(
        userId: String,
        applyJoinGroupInfo: ApplyJoinGroupInfo
    ): Result<Boolean> {
        return client.get(ServiceAuthApplyResource::class).applyToJoinGroup(userId, applyJoinGroupInfo)
    }

    override fun getRedirectInformation(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        action: String?
    ): Result<AuthApplyRedirectInfoVo> {
        return client.get(ServiceAuthApplyResource::class).getRedirectInformation(
            userId = userId,
            projectId = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode,
            action = action
        )
    }

    override fun getResourceGroupUsers(
        userId: String,
        projectId: String,
        resourceType: AuthResourceType,
        resourceCode: String,
        group: BkAuthGroup?
    ): Result<List<String>> {
        val isProjectUser = client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId,
            group = group
        ).data ?: false
        if (!isProjectUser) {
            return Result(emptyList())
        }
        return client.get(ServiceResourceMemberResource::class).getResourceGroupMembers(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = resourceType.value,
            resourceCode = resourceCode,
            group = group
        )
    }
}

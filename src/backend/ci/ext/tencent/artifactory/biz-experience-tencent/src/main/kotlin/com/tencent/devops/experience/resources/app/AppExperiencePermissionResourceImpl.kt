package com.tencent.devops.experience.resources.app

import com.tencent.devops.auth.api.service.ServiceAuthApplyResource
import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperiencePermissionResource

@RestResource
class AppExperiencePermissionResourceImpl(
    val client: Client
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
}

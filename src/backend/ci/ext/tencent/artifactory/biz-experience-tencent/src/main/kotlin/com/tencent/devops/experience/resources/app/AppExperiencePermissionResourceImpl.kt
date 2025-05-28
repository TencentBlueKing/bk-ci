package com.tencent.devops.experience.resources.app

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
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
import com.tencent.devops.experience.service.ExperiencePermissionService

@RestResource
class AppExperiencePermissionResourceImpl(
    val client: Client,
    val tokenService: ClientTokenService,
    val experiencePermissionService: ExperiencePermissionService
) : AppExperiencePermissionResource {
    override fun applyToJoinGroup(
        userId: String,
        applyJoinGroupInfo: ApplyJoinGroupInfo
    ): Result<Boolean> {
        return client.get(ServiceAuthApplyResource::class).applyToJoinGroup(userId, applyJoinGroupInfo)
    }

    override fun getApplyPermissionInformation(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        artifactoryPath: String
    ): Result<AuthApplyRedirectInfoVo?> {
        return Result(
            experiencePermissionService.getApplyPermissionInformation(
                user = userId,
                projectId = projectId,
                artifactoryType = artifactoryType,
                artifactoryPath = artifactoryPath
            )
        )
    }

    override fun getResourceGroupUsers(
        userId: String,
        projectId: String,
        resourceType: String,
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
            resourceType = resourceType,
            resourceCode = resourceCode,
            group = group
        )
    }
}

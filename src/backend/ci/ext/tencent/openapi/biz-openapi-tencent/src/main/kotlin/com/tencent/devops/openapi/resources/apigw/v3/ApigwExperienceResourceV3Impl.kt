package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.service.ServiceExperienceGroupResource
import com.tencent.devops.experience.api.service.ServiceExperienceResource
import com.tencent.devops.experience.pojo.ExperienceInfoForBuild
import com.tencent.devops.experience.pojo.ExperienceJumpInfo
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.pojo.ExperienceUpdate
import com.tencent.devops.experience.pojo.GroupCreate
import com.tencent.devops.experience.pojo.GroupUpdate
import com.tencent.devops.experience.pojo.GroupUsers
import com.tencent.devops.openapi.api.apigw.v3.ApigwExperienceResourceV3
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwExperienceResourceV3Impl @Autowired constructor(
    private val client: Client
) : ApigwExperienceResourceV3 {
    override fun jumpInfo(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        bundleIdentifier: String,
        platform: String
    ): Result<ExperienceJumpInfo> {
        return client.get(ServiceExperienceResource::class).jumpInfo(projectId, bundleIdentifier, platform)
    }

    override fun listForBuild(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<List<ExperienceInfoForBuild>> {
        return client.get(ServiceExperienceResource::class).listForBuild(userId, projectId, pipelineId, buildId)
    }

    override fun offline(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        experienceHashId: String
    ): Result<Boolean> {
        return client.get(ServiceExperienceResource::class).offline(userId, projectId, experienceHashId)
    }

    override fun online(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        experienceHashId: String
    ): Result<Boolean> {
        return client.get(ServiceExperienceResource::class).online(userId, projectId, experienceHashId)
    }

    override fun create(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        experience: ExperienceServiceCreate
    ): Result<String> {
        return client.get(ServiceExperienceResource::class).create(userId, projectId, experience)
    }

    override fun edit(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        experienceHashId: String,
        experience: ExperienceUpdate
    ): Result<Boolean> {
        return client.get(ServiceExperienceResource::class).edit(userId, projectId, experienceHashId, experience)
    }

    override fun create(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        group: GroupCreate
    ): Result<String> {
        return client.get(ServiceExperienceGroupResource::class).create(userId, projectId, group)
    }

    override fun edit(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        groupHashId: String,
        group: GroupUpdate
    ): Result<Boolean> {
        return client.get(ServiceExperienceGroupResource::class).edit(userId, projectId, groupHashId, group)
    }

    override fun delete(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        groupHashId: String
    ): Result<Boolean> {
        return client.get(ServiceExperienceGroupResource::class).delete(userId, projectId, groupHashId)
    }

    override fun getUsers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        groupHashId: String
    ): Result<GroupUsers> {
        return client.get(ServiceExperienceGroupResource::class).getUsers(userId, projectId, groupHashId)
    }
}

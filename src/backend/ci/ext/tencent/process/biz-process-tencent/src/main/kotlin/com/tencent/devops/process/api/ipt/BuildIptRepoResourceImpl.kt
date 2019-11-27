package com.tencent.devops.process.api.ipt

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildIptRepoResourceImpl @Autowired constructor(
    private val client: Client,
    private val authPermissionApi: AuthPermissionApi
) : BuildIptRepoResource {
    override fun getCommitBuildArtifactorytInfo(
        projectId: String,
        pipelineId: String,
        userId: String,
        commitId: String
    ): IptBuildArtifactoryInfo {
        checkPermission(projectId, pipelineId, userId)

        val buildId = client.get(ServiceRepositoryResource::class).getBuildIdByCommit(pipelineId, commitId).data
            ?: return IptBuildArtifactoryInfo()

        val searchProperty = listOf(Property("buildId", buildId), Property("pipelineId", pipelineId))
        val fileList = client.get(ServiceArtifactoryResource::class)
            .search(projectId, null, null, searchProperty).data?.records ?: listOf()
        return IptBuildArtifactoryInfo(buildId, fileList)
    }

    private fun checkPermission(projectId: String, pipelineId: String, userId: String) {
        val result = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = BSPipelineAuthServiceCode(),
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            permission = AuthPermission.DOWNLOAD
        )
        if (!result) throw RuntimeException("用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限")
    }
}
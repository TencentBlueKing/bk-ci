package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.builds.BuildArtifactoryResource
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.auth.api.service.ServiceAuthAuthorizationResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.springframework.beans.factory.annotation.Autowired

class BuildArtifactoryResourceImpl @Autowired constructor(
    private val archiveFileService: ArchiveFileService,
    private val client: Client
) : BuildArtifactoryResource {
    override fun acrossProjectCopy(
        projectId: String,
        pipelineId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Result<Count> {
        // pref:流水线相关的文件操作人调整为流水线的权限代持人 #11016
        val userId = client.get(ServiceAuthAuthorizationResource::class).getResourceAuthorization(
            projectId = projectId,
            resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
            resourceCode = pipelineId
        ).data?.handoverFrom ?: client.get(ServicePipelineResource::class)
            .getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser
        val count = archiveFileService.acrossProjectCopy(
            userId = userId,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = path,
            targetPath = targetPath,
            targetProjectId = targetProjectId
        )
        return Result(count)
    }
}

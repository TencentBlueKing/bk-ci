package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.builds.BuildArtifactoryResource
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.common.api.pojo.Result
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
        val userId = client.get(ServicePipelineResource::class)
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

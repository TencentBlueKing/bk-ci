package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.builds.BuildArtifactoryResource
import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.auth.api.service.ServiceAuthAuthorizationResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class BuildArtifactoryResourceImpl @Autowired constructor(
    private val archiveFileService: ArchiveFileService,
    private val client: Client
) : BuildArtifactoryResource {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildArtifactoryResourceImpl::class.java)
    }

    override fun acrossProjectCopy(
        projectId: String,
        pipelineId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        targetProjectId: String,
        targetPath: String
    ): Result<Count> {
        val userId = getPipelineHandoverUser(projectId, pipelineId)
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

    override fun show(
        projectId: String,
        pipelineId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ): Result<FileDetail> {
        val operator = getPipelineHandoverUser(projectId, pipelineId)
        return Result(archiveFileService.show(
            userId = operator,
            projectId = projectId,
            artifactoryType = artifactoryType,
            path = path
        ))
    }

    // 获取流水线的权限代持人
    private fun getPipelineHandoverUser(projectId: String, pipelineId: String): String {
        return try {
            client.get(ServiceAuthAuthorizationResource::class).getResourceAuthorization(
                projectId = projectId,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = pipelineId
            ).data
        } catch (ignored: Throwable) {
            logger.info("get pipeline oauth user fail", ignored)
            null
        }?.handoverFrom ?: client.get(ServicePipelineResource::class)
            .getPipelineInfo(projectId, pipelineId, null).data!!.lastModifyUser
    }
}

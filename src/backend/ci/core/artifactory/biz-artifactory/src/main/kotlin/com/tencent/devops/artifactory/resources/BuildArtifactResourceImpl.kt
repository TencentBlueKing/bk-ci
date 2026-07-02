package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.builds.BuildArtifactResource
import com.tencent.devops.artifactory.pojo.artifact.ArtifactMetadataRequest
import com.tencent.devops.artifactory.service.artifact.PipelineArtifactInfoService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory

/**
 * 构建产出物元数据上报 API 实现
 */
@RestResource
class BuildArtifactResourceImpl(
    private val pipelineArtifactInfoService: PipelineArtifactInfoService
) : BuildArtifactResource {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildArtifactResourceImpl::class.java)
    }

    override fun reportArtifactMetadata(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        request: ArtifactMetadataRequest
    ): Result<Boolean> {
        logger.info(
            "BuildArtifact|$userId|reportArtifactMetadata|$projectId|$pipelineId|$buildId|" +
                    "${request.artifactType}|${request.artifactName}|${request.artifactVersion}"
        )
        return kotlin.runCatching {
            pipelineArtifactInfoService.saveArtifactInfo(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                request = request
            )
            Result(true)
        }.getOrElse { e ->
            logger.error("Failed to save artifact info: ${e.message}", e)
            Result(status = 500, message = e.message ?: "Failed to save artifact info")
        }
    }
}

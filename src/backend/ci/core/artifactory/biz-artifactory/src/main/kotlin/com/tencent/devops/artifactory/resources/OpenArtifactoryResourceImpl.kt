package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.service.OpenArtifactoryResource
import com.tencent.devops.artifactory.service.PipelineBuildArtifactoryService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.process.api.service.ServicePipelineRuntimeResource
import org.slf4j.LoggerFactory

class OpenArtifactoryResourceImpl(
    private val clientTokenService: ClientTokenService,
    private val pipelineBuildArtifactoryService: PipelineBuildArtifactoryService,
    private val client: Client
) : OpenArtifactoryResource {

    override fun updateArtifactList(
        token: String,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        val validateTokenFlag = clientTokenService.checkToken(null, token)
        if (!validateTokenFlag) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(token)
            )
        }

        val artifactList = pipelineBuildArtifactoryService.getArtifactList(userId, projectId, pipelineId, buildId)
        logger.info("[$pipelineId]|getArtifactList-$buildId artifact: ${JsonUtil.toJson(artifactList)}")
        val result = client.get(ServicePipelineRuntimeResource::class).updateArtifactList(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactoryFileList = artifactList
        )
        logger.info("[$buildId]|update artifact result: ${result.status} ${result.message}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenArtifactoryResourceImpl::class.java)
    }
}
package com.tencent.devops.artifactory.resources

import com.tencent.bkrepo.webhook.pojo.payload.node.NodeCreatedEventPayload
import com.tencent.devops.artifactory.api.service.OpenArtifactoryResource
import com.tencent.devops.artifactory.service.PipelineBuildArtifactoryService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineRuntimeResource
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response

@RestResource
class OpenArtifactoryResourceImpl(
    private val clientTokenService: ClientTokenService,
    private val pipelineBuildArtifactoryService: PipelineBuildArtifactoryService,
    private val client: Client
) : OpenArtifactoryResource {

    override fun updateArtifactList(
        token: String,
        nodeCreatedEventPayload: NodeCreatedEventPayload
    ) {
        val validateTokenFlag = clientTokenService.checkToken(null, token)
        if (!validateTokenFlag) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(token)
            )
        }

        val userId = nodeCreatedEventPayload.user.userId
        val projectId = nodeCreatedEventPayload.node.projectId
        val pipelineId = nodeCreatedEventPayload.node.metadata["pipelineId"]?.toString()
        val buildId = nodeCreatedEventPayload.node.metadata["buildId"]?.toString()

        if (pipelineId == null || buildId == null) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EMPTY,
                statusCode = Response.Status.BAD_REQUEST.statusCode
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

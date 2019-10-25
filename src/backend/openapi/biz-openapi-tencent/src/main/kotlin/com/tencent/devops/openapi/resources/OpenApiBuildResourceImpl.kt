package com.tencent.devops.openapi.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.OpenApiBuildResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpenApiBuildResourceImpl @Autowired constructor(private val client: Client) : OpenApiBuildResource {

    override fun start(
        userId: String,
        projectId: String,
        pipelineId: String,
        values: Map<String, String>
    ): Result<BuildId> {
        logger.info("Start the pipeline($pipelineId) of project($projectId) with param($values) by user($userId)")
        return client.get(ServiceBuildResource::class).manualStartup(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            values = values,
            channelCode = ChannelCode.BS
        )
    }

    override fun getStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<BuildHistoryWithVars> {
        logger.info("Get the build($buildId) status of project($projectId) and pipeline($pipelineId) by user($userId)")
        return client.get(ServiceBuildResource::class).getBuildStatus(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = ChannelCode.BS
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenApiBuildResourceImpl::class.java)
    }
}
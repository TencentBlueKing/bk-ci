package com.tencent.devops.openapi.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.v2.ApigwBuildResourceV2
import com.tencent.devops.process.api.ServiceBuildResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwBuildResourceV2Impl @Autowired constructor(private val client: Client) : ApigwBuildResourceV2 {
    override fun stop(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<Boolean> {
        logger.info("Stop the build($buildId) of pipeline($pipelineId) of project($projectId) by user($userId)")
        return client.get(ServiceBuildResource::class).manualShutdown(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = ChannelCode.BS
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwBuildResourceV2Impl::class.java)
    }
}
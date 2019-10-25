package com.tencent.devops.openapi.resources

import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.ApigwBuildResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwBuildResourceImpl @Autowired constructor(private val client: Client) : ApigwBuildResource {
    override fun manualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<BuildManualStartupInfo> {
        logger.info("get the pipeline($pipelineId) of project($projectId) manual startup info  by user($userId)")
        return client.get(ServiceBuildResource::class).manualStartupInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS
        )
    }

    override fun getHistoryBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<BuildHistoryPage<BuildHistory>> {
        logger.info("get the pipeline($pipelineId) of project($projectId) build history  by user($userId)")
        return client.get(ServiceBuildResource::class).getHistoryBuild(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            page = page,
            pageSize = pageSize,
            channelCode = ChannelCode.BS
        )
    }

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
        private val logger = LoggerFactory.getLogger(ApigwBuildResourceImpl::class.java)
    }
}
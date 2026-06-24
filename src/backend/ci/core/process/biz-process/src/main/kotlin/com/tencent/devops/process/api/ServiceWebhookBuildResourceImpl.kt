package com.tencent.devops.process.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServiceWebhookBuildResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.webhook.WebhookStartPipelineRequest
import com.tencent.devops.process.pojo.webhook.WebhookTriggerParams
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceWebhookBuildResourceImpl @Autowired constructor(
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineBuildService: PipelineBuildService
) : ServiceWebhookBuildResource {

    override fun webhookTrigger(
        userId: String,
        projectId: String,
        pipelineId: String,
        params: WebhookTriggerParams,
        channelCode: ChannelCode,
        startType: StartType
    ): Result<String?> {

        val buildId = pipelineBuildFacadeService.webhookTriggerPipelineBuild(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            parameters = params.params,
            checkPermission = ChannelCode.isNeedAuth(channelCode),
            startType = startType,
            startValues = params.startValues,
            userParameters = params.userParams,
            triggerReviewers = params.triggerReviewers
        )

        return Result(buildId)
    }

    override fun webhookStartPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        request: WebhookStartPipelineRequest
    ): Result<BuildId> {
        with(request) {
            val buildId = pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = pipelineInfo,
                startType = startType,
                pipelineParamMap = pipelineParamMap,
                channelCode = channelCode,
                isMobile = false,
                resource = resource,
                signPipelineVersion = resource.version,
                frequencyLimit = frequencyLimit
            )
            return Result(buildId)
        }
    }
}

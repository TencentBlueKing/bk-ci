package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.trigger.GenericEventStartRequest
import com.tencent.devops.process.pojo.pipeline.IMateBuildStartRequest
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.trigger.market.MarketEventTriggerBuildService
import com.tencent.devops.store.pojo.common.BK_STORE_CREATIVE_STREAM_IMATE_MESSAGE_REMINDER_TRIGGER
import org.springframework.stereotype.Service

@Service
class TxPipelineBuildFacadeService(
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val marketEventTriggerBuildService: MarketEventTriggerBuildService,
    private val pipelineVisibilityService: PipelineVisibilityService
) {

    fun iMateBuildStart(
        userId: String,
        projectId: String,
        pipelineId: String,
        request: IMateBuildStartRequest
    ): BuildId {
        checkVisibility(userId, projectId, pipelineId)
        return marketEventTriggerBuildService.genericEventTrigger(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            eventCode = BK_STORE_CREATIVE_STREAM_IMATE_MESSAGE_REMINDER_TRIGGER,
            request = GenericEventStartRequest(
                eventBody = mapOf(
                    IMateBuildStartRequest::triggerUser.name to request.triggerUser,
                    IMateBuildStartRequest::message.name to request.message
                ),
                startParams = request.startParams
            )
        )
    }

    fun visibilityManualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): BuildManualStartupInfo {
        checkVisibility(userId, projectId, pipelineId)
        return pipelineBuildFacadeService.buildManualStartupInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.getRequestChannelCode(),
            checkPermission = false
        )
    }

    private fun checkVisibility(
        userId: String,
        projectId: String,
        pipelineId: String
    ) {
        if (!pipelineVisibilityService.hasVisibility(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId
            )
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_USER_NOT_VISIBLE,
                params = arrayOf(userId, pipelineId)
            )
        }
    }
}

package com.tencent.devops.process.trigger.artifact

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.trigger.event.ArtifactWebhookTriggerEvent
import com.tencent.devops.process.trigger.replay.TriggerReplayHandler
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 制品到达触发事件重放
 */
@Service
class ArtifactTriggerReplayHandler(
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val artifactEventTriggerBuildService: ArtifactEventTriggerBuildService,
    private val sampleEventDispatcher: SampleEventDispatcher
) : TriggerReplayHandler {

    override fun support(triggerType: PipelineTriggerType) = triggerType == PipelineTriggerType.ARTIFACT

    override fun replay(replayEvent: ReplayWebhookEvent, replayTriggerEvent: PipelineTriggerEvent) {
        buildTriggerEvents(
            replayEvent = replayEvent,
            sourceTriggerEvent = replayTriggerEvent
        ).forEach { sampleEventDispatcher.dispatch(it) }
    }

    override fun replayBuild(replayEvent: ReplayWebhookEvent, replayTriggerEvent: PipelineTriggerEvent): String? {
        return buildTriggerEvents(
            replayEvent = replayEvent,
            sourceTriggerEvent = replayTriggerEvent
        ).firstNotNullOfOrNull { artifactEventTriggerBuildService.artifactWebhookTrigger(it) }
    }

    /**
     * 复用源事件 eventBody 生成重放的单流水线触发事件，replayEvent.pipelineId 不为空时只生成这一条流水线
     */
    private fun buildTriggerEvents(
        replayEvent: ReplayWebhookEvent,
        sourceTriggerEvent: PipelineTriggerEvent
    ): List<ArtifactWebhookTriggerEvent> {
        logger.info("Receive artifact replay event|${JsonUtil.toJson(replayEvent, false)}")
        val eventBody = sourceTriggerEvent.eventBody as? GenericWebhookEventBody ?: run {
            logger.warn("artifact replay eventBody invalid|eventId=${sourceTriggerEvent.eventId}")
            return emptyList()
        }
        val artifactEvent = eventBody.parseBody<ArtifactEvent>() ?: run {
            logger.warn("artifact replay parse body failed|eventId=${sourceTriggerEvent.eventId}")
            return emptyList()
        }
        val eventSource = sourceTriggerEvent.eventSource ?: artifactEvent.getAssociationId()
        val eventType = sourceTriggerEvent.eventType
        val eventScopes = ArtifactWebhookUtils.buildQueryScopes(artifactEvent)
        val pipelines = resolveReplayPipelines(replayEvent, eventSource, eventType, eventScopes)
        if (pipelines.isEmpty()) {
            logger.info(
                "no pipelines for artifact replay|eventId=${replayEvent.eventId}|" +
                    "projectId=${replayEvent.projectId}|eventSource=$eventSource"
            )
            return emptyList()
        }
        val triggerUser = replayEvent.userId.ifBlank { artifactEvent.user.userId }
        return pipelines.map { pipeline ->
            ArtifactWebhookTriggerEvent(
                projectId = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                eventId = replayEvent.eventId,
                eventSource = eventSource,
                eventType = eventType,
                triggerUser = triggerUser
            )
        }
    }

    private fun resolveReplayPipelines(
        replayEvent: ReplayWebhookEvent,
        eventSource: String,
        eventType: String,
        eventScopes: List<String>?
    ): List<PipelineEventSubscriber> {
        val targetPipelineId = replayEvent.pipelineId
        return if (targetPipelineId.isNullOrBlank()) {
            pipelineEventSubscriptionDao.listEventSubscriber(
                dslContext = dslContext,
                eventSource = eventSource,
                eventType = eventType,
                eventCode = ArtifactTriggerElement.classType,
                eventScopes = eventScopes
            ).filter { it.projectId == replayEvent.projectId }
                .distinctBy { it.pipelineId }
        } else {
            listOf(
                PipelineEventSubscriber(
                    projectId = replayEvent.projectId,
                    pipelineId = targetPipelineId,
                    channelCode = ChannelCode.getRequestChannelCode()
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactTriggerReplayHandler::class.java)
    }
}

package com.tencent.devops.process.trigger.tapd

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_EVENT_SEPARATOR
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_CURRENT_USER
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_EVENT
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_WORKSPACE_ID
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.trigger.event.TapdWebhookTriggerEvent
import com.tencent.devops.process.trigger.replay.TriggerReplayHandler
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * TAPD 触发事件重放
 */
@Service
class TapdTriggerReplayHandler(
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val tapdEventTriggerBuildService: TapdEventTriggerBuildService,
    private val sampleEventDispatcher: SampleEventDispatcher
) : TriggerReplayHandler {

    override fun support(triggerType: PipelineTriggerType) = triggerType == PipelineTriggerType.TAPD

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
        ).firstNotNullOfOrNull { tapdEventTriggerBuildService.tapdWebhookTrigger(it) }
    }

    /**
     * 生成重放的单流水线触发事件，replayEvent.pipelineId 不为空时只生成这一条流水线
     */
    private fun buildTriggerEvents(
        replayEvent: ReplayWebhookEvent,
        sourceTriggerEvent: PipelineTriggerEvent
    ): List<TapdWebhookTriggerEvent> {
        logger.info("Receive tapd replay event|${JsonUtil.toJson(replayEvent, false)}")
        val body = extractReplayBody(sourceTriggerEvent) ?: return emptyList()
        val rawEvent = body[TAPD_KEY_EVENT] ?: run {
            logger.warn("tapd replay missing event field|eventId=${sourceTriggerEvent.eventId}")
            return emptyList()
        }
        val (eventType, eventAction) = parseRawEvent(rawEvent, body) ?: return emptyList()

        val workspaceId = body[TAPD_KEY_WORKSPACE_ID] ?: sourceTriggerEvent.eventSource ?: ""
        val triggerUser = replayEvent.userId.ifBlank { body[TAPD_KEY_CURRENT_USER] ?: "" }
        val pipelines = resolveReplayPipelines(
            replayEvent = replayEvent,
            workspaceId = workspaceId,
            eventType = eventType
        )
        if (pipelines.isEmpty()) {
            logger.info(
                "no pipelines for tapd replay|eventId=${replayEvent.eventId}|" +
                    "projectId=${replayEvent.projectId}|workspaceId=$workspaceId|eventType=${eventType.value}"
            )
            return emptyList()
        }
        return pipelines.map { pipeline ->
            TapdWebhookTriggerEvent(
                projectId = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                eventId = replayEvent.eventId,
                workspaceId = workspaceId,
                eventType = eventType,
                eventAction = eventAction,
                triggerUser = triggerUser
            )
        }
    }

    private fun extractReplayBody(sourceTriggerEvent: PipelineTriggerEvent): Map<String, String>? {
        val eventId = sourceTriggerEvent.eventId
        val eventBody = sourceTriggerEvent.eventBody as? GenericWebhookEventBody
        if (eventBody == null) {
            logger.warn("tapd replay source eventBody is not GenericWebhookEventBody|eventId=$eventId")
            return null
        }
        val body = eventBody.bodyAsMap()
        if (body.isNullOrEmpty()) {
            logger.warn("tapd replay source eventBody is empty|eventId=$eventId")
            return null
        }
        return body
    }

    /**
     * 解析 TAPD 原始事件字符串（例如 `story::create`）
     */
    private fun parseRawEvent(rawEvent: String, body: Map<String, Any?>): Pair<TapdEventType, TapdEventAction>? {
        val parts = rawEvent.split(TAPD_EVENT_SEPARATOR)
        if (parts.size != 2) {
            logger.warn("tapd replay invalid event format|event=$rawEvent")
            return null
        }
        return TapdWebhookUtils.parseEvent(eventTypeRaw = parts[0], eventActionRaw = parts[1], body = body)
    }

    private fun resolveReplayPipelines(
        replayEvent: ReplayWebhookEvent,
        workspaceId: String,
        eventType: TapdEventType
    ): List<PipelineEventSubscriber> {
        val targetPipelineId = replayEvent.pipelineId
        return if (targetPipelineId.isNullOrBlank()) {
            pipelineEventSubscriptionDao.listEventSubscriber(
                dslContext = dslContext,
                eventCode = TapdWebHookTriggerElement.classType,
                eventSource = workspaceId,
                eventType = eventType.value
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
        private val logger = LoggerFactory.getLogger(TapdTriggerReplayHandler::class.java)
    }
}

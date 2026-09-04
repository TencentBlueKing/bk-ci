package com.tencent.devops.process.trigger.artifact

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.pojo.webhook.BkRepoEventType
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactTriggerEventType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.pojo.trigger.artifact.NodeArtifactEvent
import com.tencent.devops.process.pojo.trigger.artifact.PackageVersionArtifactEvent
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.artifact.handler.ArtifactEventHandlerManager
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.trigger.event.ArtifactWebhookRequestEvent
import com.tencent.devops.process.trigger.event.ArtifactWebhookTriggerEvent
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 制品到达 Webhook 请求处理服务
 *
 * 1. 解析原始 webhook body（多态反序列化为 [ArtifactEvent]）；
 * 2. 推导粗粒度事件源，查订阅该事件的流水线；
 * 3. 保存触发事件（原始 body 存入 eventBody 便于回放/排查）；
 * 4. 向下游投递精简版 [ArtifactWebhookTriggerEvent]（路由 + 事件标识）。
 */
@Service
class ArtifactWebhookRequestService(
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val eventHandlerManager: ArtifactEventHandlerManager
) {
    fun handleRequest(requestEvent: ArtifactWebhookRequestEvent) {
        val rawBody = requestEvent.request.body
        val rawEventType = requestEvent.eventType
        logger.info("Receive ArtifactWebhookRequestEvent|$rawEventType")
        val artifactEvent = parseEvent(rawEventType, rawBody) ?: return
        // bkrepo 原始事件类型(NODE_CREATED/VERSION_CREATED 等)转换为产品语义事件，与订阅落库保持一致
        val eventType = convertToTriggerEventType(rawEventType).name
        logger.info("parse artifact event|$eventType|${JsonUtil.toJson(artifactEvent, false)}")
        val eventSource = artifactEvent.getAssociationId()
        val eventScopes = ArtifactWebhookUtils.buildQueryScopes(artifactEvent)
        val subscribers = pipelineEventSubscriptionDao.listEventSubscriber(
            dslContext = dslContext,
            eventSource = eventSource,
            eventType = eventType,
            eventCode = ArtifactTriggerElement.classType,
            eventScopes = eventScopes
        )
        if (subscribers.isEmpty()) {
            logger.info("no pipelines subscribed|eventSource=$eventSource|eventScopes=$eventScopes")
            return
        }
        subscribers.groupBy { it.projectId }
            .mapValues { it.value.distinctBy { subscriber -> subscriber.pipelineId } }
            .forEach { (projectId, pipelines) ->
                val triggerEvent = buildTriggerEvent(
                    projectId = projectId,
                    eventSource = eventSource,
                    eventType = eventType,
                    request = requestEvent,
                    artifactEvent = artifactEvent
                )
                try {
                    pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)
                } catch (ignored: Throwable) {
                    logger.warn("fail to save artifact trigger event|$projectId", ignored)
                    return@forEach
                }
                dispatchTriggerEvents(
                    pipelines = pipelines,
                    eventId = triggerEvent.eventId!!,
                    eventSource = eventSource,
                    eventType = eventType,
                    triggerUser = artifactEvent.user.userId
                )
            }
    }

    /**
     * 回放制品到达事件：复用源事件 eventBody，按订阅或指定流水线重新投递触发事件。
     */
    fun replay(replayEvent: ReplayWebhookEvent, sourceTriggerEvent: PipelineTriggerEvent) {
        logger.info("Receive artifact replay event|${JsonUtil.toJson(replayEvent, false)}")
        val eventBody = sourceTriggerEvent.eventBody as? GenericWebhookEventBody ?: run {
            logger.warn("artifact replay eventBody invalid|eventId=${sourceTriggerEvent.eventId}")
            return
        }
        val artifactEvent = eventBody.parseBody<ArtifactEvent>() ?: run {
            logger.warn("artifact replay parse body failed|eventId=${sourceTriggerEvent.eventId}")
            return
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
            return
        }
        dispatchTriggerEvents(
            pipelines = pipelines,
            eventId = replayEvent.eventId,
            eventSource = eventSource,
            eventType = eventType,
            triggerUser = replayEvent.userId.ifBlank { artifactEvent.user.userId }
        )
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

    private fun parseEvent(eventType: String, rawBody: String): ArtifactEvent? {
        val clazz: Class<out ArtifactEvent> = when (BkRepoEventType.fromValue(eventType)) {
            BkRepoEventType.VERSION_CREATED ->
                PackageVersionArtifactEvent::class.java

            BkRepoEventType.NODE_CREATED,
            BkRepoEventType.NODE_RENAMED,
            BkRepoEventType.NODE_MOVED,
            BkRepoEventType.NODE_COPIED,
            BkRepoEventType.NODE_DELETED,
            BkRepoEventType.NODE_DOWNLOADED,
            BkRepoEventType.METADATA_SAVED,
            BkRepoEventType.METADATA_DELETED ->
                NodeArtifactEvent::class.java

            else -> {
                logger.warn("unsupported bkrepo artifact event type|$eventType")
                return null
            }
        }
        return try {
            JsonUtil.to(rawBody, clazz)
        } catch (ignored: Throwable) {
            logger.warn("fail to parse artifact webhook body|eventType=$eventType", ignored)
            null
        }
    }

    /**
     * 将 bkrepo 原始事件类型转换为产品语义的制品触发事件类型。
     *
     * 当前产品仅定义「制品到达」一种，节点/包版本类事件统一转换为 [ArtifactTriggerEventType.ARRIVED]；
     * 后续若新增其它产品事件（如制品删除），在此扩展映射即可，无需改动订阅匹配逻辑。
     */
    private fun convertToTriggerEventType(rawEventType: String): ArtifactTriggerEventType {
        return when (BkRepoEventType.fromValue(rawEventType)) {
            BkRepoEventType.NODE_CREATED,
            BkRepoEventType.NODE_RENAMED,
            BkRepoEventType.NODE_MOVED,
            BkRepoEventType.NODE_COPIED,
            BkRepoEventType.NODE_DELETED,
            BkRepoEventType.NODE_DOWNLOADED,
            BkRepoEventType.METADATA_SAVED,
            BkRepoEventType.METADATA_DELETED,
            BkRepoEventType.VERSION_CREATED -> ArtifactTriggerEventType.ARRIVED

            else -> ArtifactTriggerEventType.ARRIVED
        }
    }

    private fun buildTriggerEvent(
        projectId: String,
        eventSource: String,
        eventType: String,
        request: ArtifactWebhookRequestEvent,
        artifactEvent: ArtifactEvent
    ): PipelineTriggerEvent {
        val requestId = MDC.get(TraceTag.BIZID) ?: ""
        val eventId = pipelineTriggerEventService.getEventId()
        val eventBody = GenericWebhookEventBody(
            headers = request.request.headers ?: emptyMap(),
            queryParams = request.request.queryParams ?: emptyMap(),
            body = request.request.body,
            bodyClazz = artifactEvent::class.java.name
        )
        return PipelineTriggerEvent(
            requestId = requestId,
            projectId = projectId,
            eventId = eventId,
            triggerType = PipelineTriggerType.ARTIFACT.name,
            eventSource = eventSource,
            eventType = eventType,
            triggerUser = artifactEvent.user.userId,
            eventDesc = eventHandlerManager.getEventDesc(artifactEvent).toJsonStr(),
            createTime = LocalDateTime.now(),
            eventBody = eventBody
        )
    }

    private fun dispatchTriggerEvents(
        pipelines: List<PipelineEventSubscriber>,
        eventId: Long,
        eventSource: String,
        eventType: String,
        triggerUser: String
    ) {
        pipelines.forEach { pipeline ->
            sampleEventDispatcher.dispatch(
                ArtifactWebhookTriggerEvent(
                    projectId = pipeline.projectId,
                    pipelineId = pipeline.pipelineId,
                    eventId = eventId,
                    eventSource = eventSource,
                    eventType = eventType,
                    triggerUser = triggerUser
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactWebhookRequestService::class.java)
    }
}

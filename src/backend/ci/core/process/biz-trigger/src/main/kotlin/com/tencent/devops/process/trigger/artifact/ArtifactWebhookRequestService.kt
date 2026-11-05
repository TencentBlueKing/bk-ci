package com.tencent.devops.process.trigger.artifact

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.pojo.webhook.BkRepoEventType
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactTriggerEventType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.METADATA_PIPELINE_ID
import com.tencent.devops.process.pojo.trigger.artifact.NodeArtifactEvent
import com.tencent.devops.process.pojo.trigger.artifact.PackageMetadataArtifactEvent
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.PipelineTriggerRateLimiter
import com.tencent.devops.process.trigger.artifact.handler.ArtifactEventHandlerManager
import com.tencent.devops.process.trigger.event.ArtifactWebhookRequestEvent
import com.tencent.devops.process.trigger.event.ArtifactWebhookTriggerEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
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
    private val eventHandlerManager: ArtifactEventHandlerManager,
    private val pipelineTriggerRateLimiter: PipelineTriggerRateLimiter
) {
    @Value("\${external.webhook.artifact.rate-limit.window-seconds:$DEFAULT_RATE_LIMIT_WINDOW_SECONDS}")
    private val rateLimitWindowSeconds: Long = DEFAULT_RATE_LIMIT_WINDOW_SECONDS

    @Value("\${external.webhook.artifact.rate-limit.max-count:$DEFAULT_RATE_LIMIT_MAX_COUNT}")
    private val rateLimitMaxCount: Int = DEFAULT_RATE_LIMIT_MAX_COUNT

    @Value("\${external.webhook.artifact.secret:#{null}}")
    private val artifactWebhookSecret: String? = null

    /**
     * 接收制品库 webhook 请求，校验后投递 [ArtifactWebhookRequestEvent]
     */
    fun receive(eventType: String?, secret: String?, body: String): Boolean {
        // body 为原始 JSON 字符串，可能带缩进/换行；打印前压成紧凑单行，解析失败则回退原文
        val compactBody = runCatching {
            JsonUtil.getObjectMapper(formatted = false).readTree(body).toString()
        }.getOrDefault(body)
        logger.info("artifact webhook request|$eventType|$compactBody")
        if (!artifactWebhookSecret.isNullOrBlank() && secret != artifactWebhookSecret) {
            logger.warn("the secret of artifact webhook is illegal")
            throw InvalidParamException(
                message = "secret illegal",
                params = arrayOf("secret")
            )
        }
        if (eventType.isNullOrBlank()) {
            logger.warn("the eventType of artifact webhook is missing")
            throw InvalidParamException(
                message = "eventType illegal",
                params = arrayOf("eventType")
            )
        }
        sampleEventDispatcher.dispatch(
            ArtifactWebhookRequestEvent(
                eventType = eventType,
                request = WebhookRequest(
                    headers = emptyMap(),
                    body = compactBody
                )
            )
        )
        return true
    }

    fun handleRequest(requestEvent: ArtifactWebhookRequestEvent) {
        val rawBody = requestEvent.request.body
        val rawEventType = requestEvent.eventType
        logger.info("Receive ArtifactWebhookRequestEvent|$rawEventType")
        val artifactEvent = parseEvent(rawEventType, rawBody) ?: return
        if (artifactEvent is PackageMetadataArtifactEvent && !hasPipelineIdMetadata(artifactEvent)) {
            logger.info(
                "skip package metadata event without pipelineId metadata|" +
                    "${artifactEvent.getAssociationId()}|${artifactEvent.packageKey}|" +
                    artifactEvent.packageVersion.name
            )
            return
        }
        // bkrepo 原始事件类型(NODE_CREATED/PACKAGE_METADATA_SAVED 等)转换为产品语义事件，与订阅落库保持一致
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
        // 同一仓库瞬间大量上传会触发大量构建，按仓库限流，超限直接丢弃
        val acquired = pipelineTriggerRateLimiter.tryAcquire(
            triggerType = PipelineTriggerType.ARTIFACT.name,
            limitKey = eventSource,
            maxCount = rateLimitMaxCount,
            windowSeconds = rateLimitWindowSeconds
        )
        if (!acquired) {
            logger.warn(
                "artifact webhook rate limited, skip trigger|eventSource=$eventSource|" +
                    "limit=$rateLimitMaxCount/${rateLimitWindowSeconds}s|eventType=$rawEventType"
            )
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


    private fun parseEvent(eventType: String, rawBody: String): ArtifactEvent? {
        val clazz: Class<out ArtifactEvent> = when (BkRepoEventType.fromValue(eventType)) {
            BkRepoEventType.PACKAGE_METADATA_SAVED ->
                PackageMetadataArtifactEvent::class.java

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
     * 镜像元数据可能分多次保存，只有带来源流水线ID的元数据才视为流水线产出的制品到达。
     */
    private fun hasPipelineIdMetadata(event: PackageMetadataArtifactEvent): Boolean {
        return event.packageVersion.packageMetadata.any {
            it.key == METADATA_PIPELINE_ID && !it.value?.toString().isNullOrBlank()
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
            BkRepoEventType.PACKAGE_METADATA_SAVED -> ArtifactTriggerEventType.ARRIVED

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
        private const val DEFAULT_RATE_LIMIT_WINDOW_SECONDS = 60L
        private const val DEFAULT_RATE_LIMIT_MAX_COUNT = 100
    }
}

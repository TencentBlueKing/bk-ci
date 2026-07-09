/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.trigger.tapd

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_EVENT_SEPARATOR
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_CHANGE_FIELDS
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_CURRENT_USER
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_ENTITY_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_EVENT
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_LABEL
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_NAME
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_OBJECT_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_OBJECT_URL
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_OWNER
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_PARENT_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_PRIORITY_LABEL
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_REFERER
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_SOURCE_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_STATUS
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_STORY_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_WORKSPACE_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_WORKSPACE_NAME
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.service.TapdSupportService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.event.TapdWebhookRequestEvent
import com.tencent.devops.process.trigger.event.TapdWebhookTriggerEvent
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.getHookField
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * TAPD Webhook 请求处理服务
 *
 * 职责：
 * 1. 解析 TAPD 原始 webhook payload、识别订阅了该事件的流水线；
 * 2. 将派生字段（workspaceName / objectUrl / objectId）写入 body，作为触发事件的原始 payload 保存；
 * 3. 只向下游投递「路由 + 事件标识」的精简版 [TapdWebhookTriggerEvent]，
 *    构建端（`TapdEventTriggerBuildService`）会通过 eventId 反查 [PipelineTriggerEvent.eventBody]
 *    拿到原始 body 后再匹配触发器、组装启动参数。
 */
@Service
class TapdWebhookRequestService(
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val tapdSupportService: TapdSupportService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TapdWebhookRequestService::class.java)
    }

    fun dispatch(body: Map<String, Any>): Result<Boolean> {
        logger.info("Receive TapdWebhook|${JsonUtil.toJson(body, false)}")
        val rawEvent = body[TAPD_KEY_EVENT]?.toString() ?: run {
            logger.warn("Tapd webhook missing event field")
            return Result(false)
        }
        val parts = rawEvent.split(TAPD_EVENT_SEPARATOR)
        if (parts.size != 2) {
            logger.warn("Tapd webhook invalid event format|event=$rawEvent")
            return Result(false)
        }
        val eventType = parts[0].lowercase()
        val eventAction = parts[1].lowercase()
        val workspaceId = body[TAPD_KEY_WORKSPACE_ID]?.toString() ?: run {
            logger.warn("Tapd webhook missing workspace_id|event=$rawEvent")
            return Result(false)
        }
        val triggerUser = body.getHookField(TAPD_KEY_CURRENT_USER)
        val tapdHost = TapdWebhookUtils.extractHost(body.getHookField(TAPD_KEY_REFERER))
        sampleEventDispatcher.dispatch(
            TapdWebhookRequestEvent(
                workspaceId = workspaceId,
                eventType = eventType,
                eventAction = eventAction,
                rawEvent = rawEvent,
                triggerUser = triggerUser,
                tapdHost = tapdHost,
                body = body
            )
        )
        return Result(true)
    }

    fun handleRequest(event: TapdWebhookRequestEvent) {
        logger.info("Receive TapdWebhookRequestEvent|${JsonUtil.toJson(event, false)}")
        val (eventType, eventAction) = parseEvent(
            eventTypeRaw = event.eventType,
            eventActionRaw = event.eventAction,
            body = event.body
        ) ?: return
        val subscribers = listSubscribers(workspaceId = event.workspaceId, eventType = eventType)
        if (subscribers.isEmpty()) {
            logger.info("no pipelines subscribed|workspaceId=${event.workspaceId}|eventType=${eventType.value}")
            return
        }
        val objectId = getEventObjectId(eventAction, event.body)
        val enrichedBody = enrichBody(
            body = event.body,
            eventType = eventType,
            eventAction = eventAction,
            workspaceId = event.workspaceId,
            objectId = objectId,
            tapdHost = event.tapdHost
        )
        val ctx = TapdWebhookEventContext(
            workspaceId = event.workspaceId,
            triggerUser = event.triggerUser,
            body = enrichedBody,
            eventType = eventType,
            eventAction = eventAction,
            objectId = objectId,
            objectUrl = enrichedBody.getHookField(TAPD_KEY_OBJECT_URL)
        )
        subscribers.groupBy { it.projectId }
            .mapValues { it.value.distinctBy { it.pipelineId } }
            .forEach { (projectId, pipelines) ->
                val triggerEvent = buildTriggerEvent(projectId = projectId, ctx = ctx)
                try {
                    pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)
                } catch (ignored: Throwable) {
                    logger.warn("fail to save tapd trigger event|$projectId", ignored)
                    return@forEach
                }
                dispatchTriggerEvents(
                    pipelines = pipelines,
                    eventId = triggerEvent.eventId!!,
                    ctx = ctx
                )
            }
    }

    /**
     * 回放 TAPD 触发事件
     *
     * 只需将精简版 [TapdWebhookTriggerEvent] 投递给下游，构建端会通过 [ReplayWebhookEvent.eventId]
     * 反查 [PipelineTriggerEvent.eventBody]，与首次触发共用同一段「解析 body -> 匹配 -> 组装启动参数」的逻辑。
     */
    fun replay(replayEvent: ReplayWebhookEvent, sourceTriggerEvent: PipelineTriggerEvent) {
        logger.info("Receive tapd replay event|${JsonUtil.toJson(replayEvent, false)}")
        val body = extractReplayBody(sourceTriggerEvent) ?: return
        val rawEvent = body[TAPD_KEY_EVENT] ?: run {
            logger.warn("tapd replay missing event field|eventId=${sourceTriggerEvent.eventId}")
            return
        }
        val (eventType, eventAction) = parseRawEvent(rawEvent, body) ?: return

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
            return
        }
        pipelines.forEach { pipeline ->
            sampleEventDispatcher.dispatch(
                TapdWebhookTriggerEvent(
                    projectId = pipeline.projectId,
                    pipelineId = pipeline.pipelineId,
                    eventId = replayEvent.eventId,
                    workspaceId = workspaceId,
                    eventType = eventType,
                    eventAction = eventAction,
                    triggerUser = triggerUser
                )
            )
        }
    }

    /**
     * 为原始 body 补充派生字段（workspaceName / objectUrl / objectId），并追加对象基础信息。
     */
    private fun enrichBody(
        body: Map<String, Any?>,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        workspaceId: String,
        objectId: String,
        tapdHost: String
    ): Map<String, Any?> {
        val objectUrl = TapdWebhookUtils.buildObjectUrl(
            tapdHost = tapdHost,
            workspaceId = workspaceId,
            objectId = objectId,
            eventType = eventType
        )
        val workspaceName = tapdSupportService.getWorkspaceInfo(workspaceId)?.name ?: ""
        val objectBaseInfo = getTapdObjectBaseInfo(
            eventType = eventType,
            eventAction = eventAction,
            workspaceId = workspaceId,
            objectId = objectId
        ).orEmpty()
        return buildMap<String, Any?> {
            putAll(body)
            putAll(objectBaseInfo)
            put(TAPD_KEY_WORKSPACE_NAME, workspaceName)
            put(TAPD_KEY_OBJECT_URL, objectUrl)
            put(TAPD_KEY_OBJECT_ID, objectId)
        }
    }

    @SuppressWarnings("NestedBlockDepth")
    private fun getTapdObjectBaseInfo(
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        workspaceId: String,
        objectId: String
    ) = if (needGetInfo(eventAction)) {
        when (eventType) {
            TapdEventType.BUG -> {
                val bugInfo = getBugInfo(workspaceId, objectId)
                bugInfo?.let {
                    val map = mutableMapOf<String, String>()
                    // tapd bug priority 需要额外转化一下，hook里面是英文，但是界面显示又为中文
                    if (!it.priorityLabel.isNullOrBlank()) {
                        map[TAPD_KEY_PRIORITY_LABEL] = getBugFieldsInfo(workspaceId)
                            ?.priorityLabel
                            ?.options
                            ?.get(it.priorityLabel) ?: ""
                    }
                    map[TAPD_KEY_LABEL] = it.label ?: ""
                    map[TAPD_KEY_OWNER] = (it.currentOwner?.removeSuffix(";") ?: "")
                    map[TAPD_KEY_NAME] = it.title ?: ""
                    map
                }
            }

            TapdEventType.STORY -> {
                getStoryInfo(workspaceId, objectId)?.let {
                    mapOf(
                        TAPD_KEY_LABEL to (it.label ?: ""),
                        TAPD_KEY_PRIORITY_LABEL to (it.priorityLabel ?: ""),
                        TAPD_KEY_OWNER to (it.owner?.removeSuffix(";") ?: ""),
                        TAPD_KEY_NAME to it.name,
                        TAPD_KEY_PARENT_ID to it.parentId
                    )
                }
            }

            else -> null
        }
    } else null

    private fun needGetInfo(eventAction: TapdEventAction) = listOf(
        TapdEventAction.ADD_COMMENT,
        TapdEventAction.UPDATE_COMMENT,
        TapdEventAction.DELETE_COMMENT,
        TapdEventAction.BUG_LINK,
        TapdEventAction.BUG_UNLINK,
        TapdEventAction.STORY_LINK,
        TapdEventAction.STORY_UNLINK,
        TapdEventAction.UPDATE,
        TapdEventAction.STATUS_CHANGE
    ).contains(eventAction)

    private fun getStoryInfo(workspaceId: String, storyId: String) = tapdSupportService.getStoryInfo(
        workspaceId = workspaceId,
        storyId = storyId
    )

    private fun getBugInfo(workspaceId: String, bugId: String) = tapdSupportService.getBugInfo(
        workspaceId = workspaceId,
        bugId = bugId
    )

    private fun getBugFieldsInfo(workspaceId: String) = tapdSupportService.getBugFieldsInfo(
        workspaceId = workspaceId
    )

    private fun extractReplayBody(sourceTriggerEvent: PipelineTriggerEvent): Map<String, String>? {
        val eventId = sourceTriggerEvent.eventId
        val eventBody = sourceTriggerEvent.eventBody as? GenericWebhookEventBody
        if (eventBody == null) {
            logger.warn("tapd replay source eventBody is not GenericWebhookEventBody|eventId=$eventId")
            return null
        }
        val body = eventBody.body
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
        return parseEvent(eventTypeRaw = parts[0], eventActionRaw = parts[1], body = body)
    }

    private fun resolveReplayPipelines(
        replayEvent: ReplayWebhookEvent,
        workspaceId: String,
        eventType: TapdEventType
    ): List<PipelineEventSubscriber> {
        val targetPipelineId = replayEvent.pipelineId
        return if (targetPipelineId.isNullOrBlank()) {
            listSubscribers(workspaceId = workspaceId, eventType = eventType)
                .filter { it.projectId == replayEvent.projectId }
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

    private fun parseEvent(
        eventTypeRaw: String,
        eventActionRaw: String,
        body: Map<String, Any?> = mapOf()
    ): Pair<TapdEventType, TapdEventAction>? {
        val eventType = TapdEventType.parse(eventTypeRaw) ?: run {
            logger.warn("Unsupported tapd event type|$eventTypeRaw")
            return null
        }
        val eventAction = TapdEventAction.parse(eventActionRaw) ?: run {
            logger.warn("Unsupported tapd event action|$eventActionRaw")
            return null
        }
        return convertEvent(eventType, eventAction, body)
    }

    /**
     * 转化事件类型及其动作
     */
    private fun convertEvent(
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        body: Map<String, Any?> = mapOf()
    ) = when {
        eventType == TapdEventType.STORY_COMMENT -> {
            TapdEventType.STORY to convertCommentAction(eventAction)
        }

        eventType == TapdEventType.BUG_COMMENT -> {
            TapdEventType.BUG to convertCommentAction(eventAction)
        }

        // 状态更新
        body.getHookField(TAPD_KEY_CHANGE_FIELDS).contains(TAPD_KEY_STATUS) -> {
            eventType to TapdEventAction.STATUS_CHANGE
        }

        else -> eventType to eventAction
    }

    private fun convertCommentAction(eventAction: TapdEventAction) = when (eventAction) {
        TapdEventAction.ADD -> TapdEventAction.ADD_COMMENT
        TapdEventAction.UPDATE -> TapdEventAction.UPDATE_COMMENT
        TapdEventAction.DELETE -> TapdEventAction.DELETE_COMMENT
        else -> eventAction
    }

    private fun getEventObjectId(
        eventAction: TapdEventAction,
        body: Map<String, Any?>
    ) = when (eventAction) {
        TapdEventAction.ADD_COMMENT, TapdEventAction.UPDATE_COMMENT, TapdEventAction.DELETE_COMMENT -> {
            body.getHookField(TAPD_KEY_ENTITY_ID)
        }

        TapdEventAction.STORY_LINK, TapdEventAction.STORY_UNLINK -> {
            body.getHookField(TAPD_KEY_SOURCE_ID)
        }

        TapdEventAction.BUG_LINK, TapdEventAction.BUG_UNLINK -> {
            body.getHookField(TAPD_KEY_STORY_ID)
        }

        else -> body.getHookField(TAPD_KEY_ID, eventAction == TapdEventAction.UPDATE)
    }

    private fun listSubscribers(
        workspaceId: String,
        eventType: TapdEventType
    ): List<PipelineEventSubscriber> {
        return pipelineEventSubscriptionDao.listEventSubscriber(
            dslContext = dslContext,
            eventCode = TapdWebHookTriggerElement.classType,
            eventSource = workspaceId,
            eventType = eventType.value
        )
    }

    /**
     * 向下游投递精简版触发事件
     *
     * 只投递路由 + 事件标识信息，不再预算 startParams：
     * - 减少 MQ 消息体大小；
     * - 匹配失败时避免无谓的启动参数组装开销；
     * - 首次触发与 replay 路径共用同一段「查 triggerEvent -> 匹配 -> 组装启动参数」逻辑。
     */
    private fun dispatchTriggerEvents(
        pipelines: List<PipelineEventSubscriber>,
        eventId: Long,
        ctx: TapdWebhookEventContext
    ) {
        pipelines.forEach { pipeline ->
            sampleEventDispatcher.dispatch(
                TapdWebhookTriggerEvent(
                    projectId = pipeline.projectId,
                    pipelineId = pipeline.pipelineId,
                    eventId = eventId,
                    workspaceId = ctx.workspaceId,
                    eventType = ctx.eventType,
                    eventAction = ctx.eventAction,
                    triggerUser = ctx.triggerUser
                )
            )
        }
    }

    private fun buildTriggerEvent(
        projectId: String,
        ctx: TapdWebhookEventContext
    ): PipelineTriggerEvent {
        val requestId = MDC.get(TraceTag.BIZID) ?: ""
        val eventId = pipelineTriggerEventService.getEventId()
        val eventDesc = TapdWebhookUtils.buildEventDesc(
            eventType = ctx.eventType,
            eventAction = ctx.eventAction,
            triggerUser = ctx.triggerUser,
            objectId = ctx.objectId,
            objectUrl = ctx.objectUrl
        )
        // 用通用 webhook eventBody 记录原始 payload（含派生字段），便于回放/排查
        val eventBody = GenericWebhookEventBody(
            headers = mapOf(),
            body = ctx.body.mapValues { it.value?.toString() ?: "" },
            queryParams = mapOf()
        )
        return PipelineTriggerEvent(
            requestId = requestId,
            projectId = projectId,
            eventId = eventId,
            triggerType = PipelineTriggerType.TAPD.name,
            eventSource = ctx.workspaceId,
            eventType = ctx.eventType.value,
            triggerUser = ctx.triggerUser,
            eventDesc = eventDesc,
            createTime = LocalDateTime.now(),
            eventBody = eventBody
        )
    }
}

/**
 * TAPD Webhook 处理过程中的事件上下文
 *
 * 一次 dispatch 中的事件级公共信息：
 * - 从原始 [TapdWebhookRequestEvent] 解析而来；
 * - `body` 已经写入派生字段（workspaceName / objectUrl / objectId）；
 * - 复用于订阅查询、触发事件保存、精简版触发事件下发等环节。
 */
private data class TapdWebhookEventContext(
    val workspaceId: String,
    val triggerUser: String,
    val body: Map<String, Any?>,
    val eventType: TapdEventType,
    val eventAction: TapdEventAction,
    val objectId: String,
    val objectUrl: String
)

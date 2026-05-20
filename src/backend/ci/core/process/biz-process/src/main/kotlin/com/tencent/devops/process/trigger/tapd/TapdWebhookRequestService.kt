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

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_CREATE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_DELETE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_UPDATE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_CREATE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_DELETE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_UPDATE_EVENT_DESC
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ACTION
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_EVENT_FROM
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_EVENT_URL
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_PARENT_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_PRIORITY_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_WORKSPACE_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_BUG_URL_PATTERN
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_EVENT_SEPARATOR
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_CURRENT_USER
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_ENTITY_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_EVENT
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_EVENT_FROM
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_LABEL
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_NAME
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_NEW_PREFIX
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_OWNER
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_PARENT_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_PRIORITY
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_PRIORITY_LABEL
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_REFERER
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_TARGET_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_WORKSPACE_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_STORY_URL_PATTERN
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.service.TapdSupportService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.event.TapdWebhookRequestEvent
import com.tencent.devops.process.trigger.event.TapdWebhookTriggerEvent
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.net.URI
import java.time.LocalDateTime

/**
 * TAPD Webhook 请求处理服务
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
        val tapdProjectId = body[TAPD_KEY_WORKSPACE_ID]?.toString() ?: run {
            logger.warn("Tapd webhook missing workspace_id|event=$rawEvent")
            return Result(false)
        }
        val triggerUser = body.getHookField(TAPD_KEY_CURRENT_USER)
        // 提取host
        val tapdHost = extractHost(body.getHookField(TAPD_KEY_REFERER))
        sampleEventDispatcher.dispatch(
            TapdWebhookRequestEvent(
                tapdProjectId = tapdProjectId,
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
            eventActionRaw = event.eventAction
        ) ?: return
        // 1. 通过 T_PIPELINE_EVENT_SUBSCRIPTION 查询订阅了该 (tapdProjectId + eventType) 的流水线
        val subscribers = listSubscribers(tapdProjectId = event.tapdProjectId, eventType = eventType)
        if (subscribers.isEmpty()) {
            logger.info("no pipelines subscribed|tapdProjectId=${event.tapdProjectId}|eventType=${eventType.value}")
            return
        }
        val objectId = event.body.getHookField(TAPD_KEY_ID, eventAction == TapdEventAction.UPDATE)
        // 仅生成一次工单详情页 URL，复用于触发事件描述和流水线启动参数（CI_EVENT_URL）
        val objectUrl = buildObjectUrl(
            tapdHost = event.tapdHost,
            workspaceId = event.tapdProjectId,
            objectId = objectId,
            eventType = eventType
        )
        // 追加部分基础信息，填充到原始event中
        val finalEvent = getTapdObjectBaseInfo(
            eventType = eventType,
            eventAction = eventAction,
            workspaceId = event.tapdProjectId,
            objectId = objectId
        )?.let {
            event.copy(body = event.body.plus(it))
        } ?: event
        // 2. 按 projectId 分组保存触发事件，并为每条流水线投递触发事件
        subscribers.groupBy { it.projectId }.forEach { (projectId, pipelines) ->
            val triggerEvent = buildTriggerEvent(
                projectId = projectId,
                event = finalEvent,
                eventType = eventType,
                eventAction = eventAction,
                objectId = objectId,
                objectUrl = objectUrl
            )
            try {
                pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)
            } catch (ignored: Throwable) {
                logger.warn("fail to save tapd trigger event|$projectId", ignored)
                return@forEach
            }
            dispatchTriggerEvents(
                pipelines = pipelines,
                eventId = triggerEvent.eventId ?: 0L,
                tapdProjectId = finalEvent.tapdProjectId,
                triggerUser = finalEvent.triggerUser,
                body = finalEvent.body,
                eventType = eventType,
                eventAction = eventAction,
                objectId = objectId,
                objectUrl = objectUrl
            )
        }
    }

    /**
     * 回放 TAPD 触发事件
     */
    fun replay(replayEvent: ReplayWebhookEvent, sourceTriggerEvent: PipelineTriggerEvent) {
        logger.info("Receive tapd replay event|${JsonUtil.toJson(replayEvent, false)}")
        val body = extractReplayBody(sourceTriggerEvent) ?: return
        val rawEvent = body[TAPD_KEY_EVENT] ?: run {
            logger.warn("tapd replay missing event field|eventId=${sourceTriggerEvent.eventId}")
            return
        }
        val (eventType, eventAction) = parseRawEvent(rawEvent) ?: return

        val tapdProjectId = body[TAPD_KEY_WORKSPACE_ID] ?: sourceTriggerEvent.eventSource ?: ""
        val triggerUser = replayEvent.userId.ifBlank { body[TAPD_KEY_CURRENT_USER] ?: "" }
        val objectId = getEventObjectId(eventAction, body)
        val pipelines = resolveReplayPipelines(
            replayEvent = replayEvent,
            tapdProjectId = tapdProjectId,
            eventType = eventType
        )
        if (pipelines.isEmpty()) {
            logger.info(
                "no pipelines for tapd replay|eventId=${replayEvent.eventId}|" +
                        "projectId=${replayEvent.projectId}|tapdProjectId=$tapdProjectId|eventType=${eventType.value}"
            )
            return
        }
        dispatchTriggerEvents(
            pipelines = pipelines,
            eventId = replayEvent.eventId,
            tapdProjectId = tapdProjectId,
            triggerUser = triggerUser,
            body = body,
            eventType = eventType,
            eventAction = eventAction,
            objectId = objectId,
            objectUrl = buildObjectUrl(
                tapdHost = extractHost(body.getHookField(TAPD_KEY_REFERER)),
                workspaceId = tapdProjectId,
                objectId = objectId,
                eventType = eventType
            )
        )
    }

    private fun getTapdObjectBaseInfo(
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        workspaceId: String,
        objectId: String
    ) = if (needGetInfo(eventAction)) {
        when (eventType) {
            TapdEventType.BUG -> {
                getBugInfo(workspaceId, objectId)?.let {
                    mapOf(
                        TAPD_KEY_LABEL to (it.label ?: ""),
                        TAPD_KEY_PRIORITY_LABEL to (it.priorityLabel ?: ""),
                        TAPD_KEY_OWNER to (it.currentOwner ?: "")
                    )
                }
            }

            TapdEventType.STORY -> {
                getStoryInfo(workspaceId, objectId)?.let {
                    mapOf(
                        TAPD_KEY_LABEL to (it.label ?: ""),
                        TAPD_KEY_PRIORITY_LABEL to (it.priorityLabel ?: ""),
                        TAPD_KEY_OWNER to (it.owner ?: "")
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
    ).contains(eventAction)

    private fun getStoryInfo(workspaceId: String, storyId: String) = tapdSupportService.getStoryInfo(
        workspaceId = workspaceId,
        storyId = storyId
    )

    private fun getBugInfo(workspaceId: String, bugId: String) = tapdSupportService.getBugInfo(
        workspaceId = workspaceId,
        bugId = bugId
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
    private fun parseRawEvent(rawEvent: String): Pair<TapdEventType, TapdEventAction>? {
        val parts = rawEvent.split(TAPD_EVENT_SEPARATOR)
        if (parts.size != 2) {
            logger.warn("tapd replay invalid event format|event=$rawEvent")
            return null
        }
        return parseEvent(eventTypeRaw = parts[0], eventActionRaw = parts[1])
    }

    private fun resolveReplayPipelines(
        replayEvent: ReplayWebhookEvent,
        tapdProjectId: String,
        eventType: TapdEventType
    ): List<PipelineEventSubscriber> {
        val targetPipelineId = replayEvent.pipelineId
        return if (targetPipelineId.isNullOrBlank()) {
            listSubscribers(tapdProjectId = tapdProjectId, eventType = eventType)
                    .filter { it.projectId == replayEvent.projectId }
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
        eventActionRaw: String
    ): Pair<TapdEventType, TapdEventAction>? {
        val eventType = TapdEventType.parse(eventTypeRaw) ?: run {
            logger.warn("Unsupported tapd event type|$eventTypeRaw")
            return null
        }
        val eventAction = TapdEventAction.parse(eventActionRaw) ?: run {
            logger.warn("Unsupported tapd event action|$eventActionRaw")
            return null
        }
        return convertEvent(eventType, eventAction)
    }

    /**
     * 转化事件类型及其动作
     */
    private fun convertEvent(eventType: TapdEventType, eventAction: TapdEventAction) = when (eventType) {
        TapdEventType.STORY_COMMENT -> {
            TapdEventType.STORY to convertCommentAction(eventAction)
        }

        TapdEventType.BUG_COMMENT -> {
            TapdEventType.BUG to convertCommentAction(eventAction)
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
        TapdEventAction.ADD_COMMENT,
        TapdEventAction.UPDATE_COMMENT,
        TapdEventAction.DELETE_COMMENT -> {
            body.getHookField(TAPD_KEY_ENTITY_ID)
        }

        TapdEventAction.BUG_LINK,
        TapdEventAction.BUG_UNLINK,
        TapdEventAction.STORY_LINK,
        TapdEventAction.STORY_UNLINK -> {
            body.getHookField(TAPD_KEY_TARGET_ID)
        }

        else -> body.getHookField(TAPD_KEY_ID, eventAction == TapdEventAction.UPDATE)
    }

    private fun listSubscribers(
        tapdProjectId: String,
        eventType: TapdEventType
    ): List<PipelineEventSubscriber> {
        return pipelineEventSubscriptionDao.listEventSubscriber(
            dslContext = dslContext,
            eventCode = eventType.value,
            eventSource = tapdProjectId,
            eventType = eventType.value
        )
    }

    private fun dispatchTriggerEvents(
        pipelines: List<PipelineEventSubscriber>,
        eventId: Long,
        tapdProjectId: String,
        triggerUser: String,
        body: Map<String, Any?>,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        objectId: String,
        objectUrl: String
    ) {
        val startParams = buildStartParams(
            tapdProjectId = tapdProjectId,
            triggerUser = triggerUser,
            body = body,
            eventType = eventType,
            eventAction = eventAction,
            objectId = objectId,
            objectUrl = objectUrl
        )
        val update = eventAction == TapdEventAction.UPDATE
        pipelines.forEach { pipeline ->
            sampleEventDispatcher.dispatch(
                TapdWebhookTriggerEvent(
                    projectId = pipeline.projectId,
                    pipelineId = pipeline.pipelineId,
                    eventId = eventId,
                    tapdProjectId = tapdProjectId,
                    eventType = eventType,
                    eventAction = eventAction,
                    triggerUser = triggerUser,
                    startParams = startParams,
                    triggerPriority = body.getHookField(TAPD_KEY_PRIORITY_LABEL, update),
                    triggerLabels = body.getHookField(TAPD_KEY_LABEL, update),
                    triggerOwner = body.getHookField(TAPD_KEY_OWNER, update)
                )
            )
        }
    }

    /**
     * 流水线启动参数
     */
    private fun buildStartParams(
        tapdProjectId: String,
        triggerUser: String,
        body: Map<String, Any?>,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        objectId: String,
        objectUrl: String
    ): Map<String, String> {
        val update = eventAction == TapdEventAction.UPDATE
        val params = mutableMapOf(
            CI_ACTION to eventAction.value,
            CI_EVENT_URL to objectUrl,
            CI_EVENT_FROM to body.getHookField(TAPD_KEY_EVENT_FROM),
            CI_TAPD_WORKSPACE_ID to tapdProjectId,
            CI_TAPD_ID to objectId,
            CI_TAPD_PARENT_ID to body.getHookField(TAPD_KEY_PARENT_ID, update),
            CI_TAPD_PRIORITY_ID to body.getHookField(TAPD_KEY_PRIORITY, update),
            PIPELINE_BUILD_MSG to buildPipelineBuildMsg(
                name = body.getHookField(TAPD_KEY_NAME, update),
                eventType = eventType,
                eventAction = eventAction,
                objectId = objectId
            ),
            PIPELINE_WEBHOOK_EVENT_TYPE to eventType.value,
            PIPELINE_START_WEBHOOK_USER_ID to triggerUser
        )
        return params
    }

    private fun Map<String, Any?>.getHookField(
        key: String,
        update: Boolean = false
    ): String {
        val finalKey = if (update) {
            "${TAPD_KEY_NEW_PREFIX}_$key"
        } else {
            key
        }
        return this[finalKey]?.toString() ?: ""
    }

    private fun buildPipelineBuildMsg(
        name: String,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        objectId: String
    ): String {
        val title = if (eventAction == TapdEventAction.DELETE) {
            objectId
        } else {
            name
        }
        return "[${eventAction.value} ${eventType.value}] $title"
    }

    private fun buildTriggerEvent(
        projectId: String,
        event: TapdWebhookRequestEvent,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        objectId: String,
        objectUrl: String
    ): PipelineTriggerEvent {
        val requestId = MDC.get(TraceTag.BIZID) ?: ""
        val eventId = pipelineTriggerEventService.getEventId()
        val eventDesc = getEventDesc(
            event = event,
            eventType = eventType,
            eventAction = eventAction,
            objectId = objectId,
            objectUrl = objectUrl
        )
        // 用通用 webhook eventBody 记录原始 payload，便于回放/排查
        val eventBody = GenericWebhookEventBody(
            headers = mapOf(),
            body = event.body.mapValues { it.value?.toString() ?: "" },
            queryParams = mapOf()
        )
        return PipelineTriggerEvent(
            requestId = requestId,
            projectId = projectId,
            eventId = eventId,
            triggerType = PipelineTriggerType.TAPD.name,
            eventSource = event.tapdProjectId,
            eventType = eventType.value,
            triggerUser = event.triggerUser,
            eventDesc = eventDesc,
            createTime = LocalDateTime.now(),
            eventBody = eventBody
        )
    }

    private fun getEventDesc(
        event: TapdWebhookRequestEvent,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        objectId: String,
        objectUrl: String
    ): String {
        val i18nCode = getEventDescI18nCode(eventType = eventType, eventAction = eventAction)
        return I18Variable(
            code = i18nCode,
            params = listOf(objectUrl, objectId, event.triggerUser)
        ).toJsonStr()
    }

    private fun getEventDescI18nCode(
        eventType: TapdEventType,
        eventAction: TapdEventAction
    ): String = when (eventType) {
        TapdEventType.STORY -> when (eventAction) {
            TapdEventAction.CREATE -> BK_TAPD_STORY_CREATE_EVENT_DESC
            TapdEventAction.UPDATE -> BK_TAPD_STORY_UPDATE_EVENT_DESC
            TapdEventAction.DELETE -> BK_TAPD_STORY_DELETE_EVENT_DESC
            // STORY 类型下其他动作（如 add/link 等）当前未提供专属描述，复用更新描述兜底
            else -> ""
        }

        TapdEventType.BUG -> when (eventAction) {
            TapdEventAction.CREATE -> BK_TAPD_BUG_CREATE_EVENT_DESC
            TapdEventAction.UPDATE -> BK_TAPD_BUG_UPDATE_EVENT_DESC
            TapdEventAction.DELETE -> BK_TAPD_BUG_DELETE_EVENT_DESC
            else -> ""
        }
        // 其他事件类型当前未提供专属描述，前端展示时退化为通用文案
        else -> ""
    }

    private fun buildObjectUrl(
        tapdHost: String,
        workspaceId: String,
        objectId: String,
        eventType: TapdEventType
    ): String {
        if (tapdHost.isBlank() || workspaceId.isBlank() || objectId.isBlank()) {
            return ""
        }
        // 仅 STORY/BUG 提供详情页跳转，其他事件类型暂不构造链接
        val pattern = when (eventType) {
            TapdEventType.STORY -> TAPD_STORY_URL_PATTERN
            TapdEventType.BUG -> TAPD_BUG_URL_PATTERN
            else -> return ""
        }
        return pattern.format(tapdHost.trimEnd('/'), workspaceId, objectId)
    }

    private fun extractHost(referer: String?): String {
        if (referer.isNullOrBlank()) {
            return ""
        }
        return try {
            val uri = URI(referer)
            val scheme = uri.scheme
            val host = uri.host
            when {
                scheme.isNullOrBlank() || host.isNullOrBlank() -> ""
                uri.port > 0 -> "$scheme://$host:${uri.port}"
                else -> "$scheme://$host"
            }
        } catch (ignored: Exception) {
            logger.warn("fail to parse tapd referer|referer=$referer", ignored)
            ""
        }
    }
}

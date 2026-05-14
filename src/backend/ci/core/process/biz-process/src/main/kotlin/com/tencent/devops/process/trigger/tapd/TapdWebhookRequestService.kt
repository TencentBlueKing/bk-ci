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
import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_CREATE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_DELETE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_UPDATE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_CREATE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_DELETE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_UPDATE_EVENT_DESC
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.event.TapdWebhookRequestEvent
import com.tencent.devops.process.trigger.event.TapdWebhookTriggerEvent
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
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
    private val sampleEventDispatcher: SampleEventDispatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TapdWebhookRequestService::class.java)

        // ---------- TAPD payload 字段约定 ----------
        // 事件标识字段名，例如 story::create / bug::update
        private const val TAPD_EVENT_KEY = "event"
        // 事件分隔符
        private const val TAPD_EVENT_SEPARATOR = "::"
        // 项目ID字段名（workspace_id）
        private const val TAPD_WORKSPACE_ID_KEY = "workspace_id"
        // 触发用户字段名
        private const val TAPD_CURRENT_USER_KEY = "current_user"
        // 工单ID字段名（创建/删除事件用 id；更新事件用 new_id）
        private const val TAPD_OBJECT_ID_KEY = "id"
        private const val TAPD_NEW_OBJECT_ID_KEY = "new_id"
        // 工单标题字段（更新事件可能携带 new_name；创建/历史快照携带 name）
        private const val TAPD_NAME_KEY = "name"
        private const val TAPD_NEW_NAME_KEY = "new_name"
        // 用于从中提取 TAPD 主机地址（schema + host）
        private const val TAPD_REFERER_KEY = "referer"

        // ---------- TAPD 工单详情页 URL 模板 ----------
        // 需求详情：{tapdHost}/tapd_fe/{workspaceId}/story/detail/{id}
        private const val TAPD_STORY_URL_PATTERN = "%s/tapd_fe/%s/story/detail/%s"
        // 缺陷详情：{tapdHost}/tapd_fe/{workspaceId}/bug/detail/{id}
        private const val TAPD_BUG_URL_PATTERN = "%s/tapd_fe/%s/bug/detail/%s"

        // ---------- TAPD 触发上下文启动参数 key ----------
        const val BK_CI_TAPD_PROJECT_ID = "BK_CI_TAPD_PROJECT_ID"
        const val BK_CI_TAPD_EVENT_TYPE = "BK_CI_TAPD_EVENT_TYPE"
        const val BK_CI_TAPD_EVENT_ACTION = "BK_CI_TAPD_EVENT_ACTION"
        const val BK_CI_TAPD_TRIGGER_USER = "BK_CI_TAPD_TRIGGER_USER"
        const val BK_CI_TAPD_RAW_EVENT = "BK_CI_TAPD_RAW_EVENT"
        const val BK_CI_TAPD_OBJECT_ID = "BK_CI_TAPD_OBJECT_ID"
    }

    fun dispatch(body: Map<String, Any>): Result<Boolean> {
        val rawEvent = body[TAPD_EVENT_KEY]?.toString() ?: run {
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
        val tapdProjectId = body[TAPD_WORKSPACE_ID_KEY]?.toString() ?: run {
            logger.warn("Tapd webhook missing workspace_id|event=$rawEvent")
            return Result(false)
        }
        val triggerUser = body[TAPD_CURRENT_USER_KEY]?.toString() ?: ""
        // 提取host
        val tapdHost = extractHost(body[TAPD_REFERER_KEY]?.toString())
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
        val eventType = TapdEventType.parse(event.eventType) ?: run {
            logger.warn("Unsupported tapd event type|${event.eventType}")
            return
        }
        val eventAction = TapdEventAction.parse(event.eventAction) ?: run {
            logger.warn("Unsupported tapd event action|${event.eventAction}")
            return
        }
        // 1. 通过 T_PIPELINE_EVENT_SUBSCRIPTION 查询订阅了该 (tapdProjectId + eventType) 的流水线
        val subscribers = pipelineEventSubscriptionDao.listEventSubscriber(
            dslContext = dslContext,
            eventCode = eventType.value,
            eventSource = event.tapdProjectId,
            eventType = eventType.value
        )
        if (subscribers.isEmpty()) {
            logger.info("no pipelines subscribed|tapdProjectId=${event.tapdProjectId}|eventType=${eventType.value}")
            return
        }
        val objectId = event.body[TAPD_OBJECT_ID_KEY]?.toString()
            ?: event.body[TAPD_NEW_OBJECT_ID_KEY]?.toString()
            ?: ""
        // 2. 按 projectId 分组保存触发事件，并为每条流水线投递触发事件
        val groupedByProject = subscribers.groupBy { it.projectId }
        groupedByProject.forEach { (projectId, pipelines) ->
            val triggerEvent = buildTriggerEvent(
                projectId = projectId,
                event = event,
                eventType = eventType,
                eventAction = eventAction,
                objectId = objectId
            )
            try {
                pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)
            } catch (ignored: Throwable) {
                logger.warn("fail to save tapd trigger event|$projectId", ignored)
                return@forEach
            }
            val eventId = triggerEvent.eventId ?: 0L
            // 3. 预构建启动参数，并为每条流水线分发单流水线触发事件
            val startParams = buildStartParams(
                event = event,
                eventType = eventType,
                eventAction = eventAction,
                objectId = objectId
            )
            pipelines.forEach { pipeline ->
                sampleEventDispatcher.dispatch(
                    TapdWebhookTriggerEvent(
                        projectId = projectId,
                        pipelineId = pipeline.pipelineId,
                        eventId = eventId,
                        tapdProjectId = event.tapdProjectId,
                        eventType = event.eventType,
                        eventAction = event.eventAction,
                        triggerUser = event.triggerUser,
                        startParams = startParams
                    )
                )
            }
        }
    }

    /**
     * 预构建流水线启动参数
     */
    private fun buildStartParams(
        event: TapdWebhookRequestEvent,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        objectId: String
    ): Map<String, String> {
        val params = mutableMapOf(
            BK_CI_TAPD_PROJECT_ID to event.tapdProjectId,
            BK_CI_TAPD_EVENT_TYPE to eventType.value,
            BK_CI_TAPD_EVENT_ACTION to eventAction.value,
            BK_CI_TAPD_TRIGGER_USER to event.triggerUser,
            BK_CI_TAPD_RAW_EVENT to event.rawEvent,
            PIPELINE_BUILD_MSG to buildPipelineBuildMsg(
                body = event.body,
                eventType = eventType,
                eventAction = eventAction,
                objectId = objectId
            )
        )
        if (objectId.isNotBlank()) {
            params[BK_CI_TAPD_OBJECT_ID] = objectId
        }
        return params
    }

    private fun buildPipelineBuildMsg(
        body: Map<String, Any?>,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        objectId: String
    ): String {
        val fallback = "${eventAction.value} ${eventType.value}[$objectId]"
        if (eventAction == TapdEventAction.DELETE) {
            return fallback
        }
        val name = body[TAPD_NEW_NAME_KEY]?.toString()?.takeIf { it.isNotBlank() }
            ?: body[TAPD_NAME_KEY]?.toString()?.takeIf { it.isNotBlank() }
        return name ?: fallback
    }

    /**
     * 构建 [PipelineTriggerEvent]，使用 [PipelineTriggerEventService.getEventId] 申请 eventId
     */
    private fun buildTriggerEvent(
        projectId: String,
        event: TapdWebhookRequestEvent,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        objectId: String
    ): PipelineTriggerEvent {
        val requestId = MDC.get(TraceTag.BIZID) ?: ""
        val eventId = pipelineTriggerEventService.getEventId()
        val eventDesc = getEventDesc(
            event = event,
            eventType = eventType,
            eventAction = eventAction,
            objectId = objectId
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
            triggerType = "TAPD",
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
        objectId: String
    ): String {
        val objectUrl = buildObjectUrl(
            tapdHost = event.tapdHost,
            workspaceId = event.tapdProjectId,
            objectId = objectId,
            eventType = eventType
        )
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

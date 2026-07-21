package com.tencent.devops.process.trigger.tapd

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_ADD_COMMENT_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_CREATE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_DELETE_COMMENT_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_DELETE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_STATUS_CHANGE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_UPDATE_COMMENT_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_BUG_UPDATE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_GENERIC_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_ADD_COMMENT_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_BUG_LINK_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_BUG_UNLINK_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_CREATE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_DELETE_COMMENT_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_DELETE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_LINK_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_STATUS_CHANGE_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_UNLINK_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_UPDATE_COMMENT_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STORY_UPDATE_EVENT_DESC
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_ALIAS_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_NOTE_COMMENT
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ACTION
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_EVENT_FROM
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_EVENT_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_EVENT_URL
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_LINK_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_LINK_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_PARENT_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_PRIORITY_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_TITLE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_WORKSPACE_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_BUG_URL_PATTERN
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_BUG_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_DESCRIPTION
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_EVENT_FROM
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_EVENT_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_NAME
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_NEW_PREFIX
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_OBJECT_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_OBJECT_URL
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_PARENT_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_PRIORITY_LABEL
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_TARGET_ID
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_TITLE
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_KEY_WORKSPACE_NAME
import com.tencent.devops.process.constant.TapdWebhookConstant.TAPD_STORY_URL_PATTERN
import com.tencent.devops.process.trigger.event.TapdWebhookTriggerEvent
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.buildEventDesc
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.buildObjectUrl
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.buildPipelineBuildMsg
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.buildStartParams
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.extractHost
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.getHookField
import com.tencent.devops.process.trigger.tapd.TapdWebhookUtils.getTitle
import com.tencent.devops.process.utils.BK_CI_MATERIAL_ID
import com.tencent.devops.process.utils.BK_CI_MATERIAL_NAME
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * TAPD Webhook 工具类
 *
 * 汇集 TAPD webhook 请求与触发过程中所需的纯计算工具：
 * - body 字段读取（[getHookField]）
 * - 派生字段计算（[buildObjectUrl] / [extractHost] / [getTitle]）
 * - 启动参数组装（[buildStartParams]）
 * - 事件描述组装（[buildEventDesc]）
 * - 触发消息生成（[buildPipelineBuildMsg]）
 *
 * 说明：
 * - `workspaceName` / `objectUrl` / `objectId` 会在 `TapdWebhookRequestService` 阶段写入
 *   `GenericWebhookEventBody.body`，因此本工具类可完全脱离外部依赖，是无状态纯函数集合。
 */
@Suppress("TooManyFunctions")
object TapdWebhookUtils {

    private val logger = LoggerFactory.getLogger(TapdWebhookUtils::class.java)

    /**
     * 从 body 中读取字段
     *
     * @param key 字段名
     * @param update 若为 true，则读取 `new_{key}`（TAPD 更新事件下大部分字段为 new_ 前缀）
     */
    fun Map<String, Any?>.getHookField(
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

    /**
     * 组装流水线启动参数
     *
     * 除了 TAPD 事件自身的启动变量外，还补充了 [PIPELINE_WEBHOOK_TYPE] / [BK_REPO_WEBHOOK_REPO_TYPE] /
     * [PIPELINE_GIT_EVENT_URL] 等参数：
     * 1. 让 `BuildHistory.startType` 在通过 `StartType.transform` 后能识别为 TAPD 触发；
     * 2. 让 `WebhookInfo`（构建历史触发材料区）复用现有字段展示工单详情页 URL 与 TAPD 项目名。
     */
    fun buildStartParams(
        event: TapdWebhookTriggerEvent,
        body: Map<String, String>,
        element: Element,
    ): Map<String, String> {
        val update = event.eventAction == TapdEventAction.UPDATE
        val title = getTitle(eventType = event.eventType, body = body, update = update)
        val objectId = body.getHookField(TAPD_KEY_OBJECT_ID)
        val objectUrl = body.getHookField(TAPD_KEY_OBJECT_URL)
        val workspaceName = body.getHookField(TAPD_KEY_WORKSPACE_NAME)
        val params = mutableMapOf(
            CI_ACTION to event.eventAction.value,
            CI_EVENT_URL to objectUrl,
            CI_EVENT_FROM to body.getHookField(TAPD_KEY_EVENT_FROM),
            CI_EVENT_ID to body.getHookField(TAPD_KEY_EVENT_ID),
            CI_TAPD_WORKSPACE_ID to event.workspaceId,
            CI_TAPD_ID to objectId,
            CI_TAPD_PARENT_ID to body.getHookField(TAPD_KEY_PARENT_ID, update),
            CI_TAPD_PRIORITY_ID to body.getHookField(TAPD_KEY_PRIORITY_LABEL, update),
            CI_TAPD_TITLE to title,
            PIPELINE_BUILD_MSG to buildPipelineBuildMsg(
                name = title,
                eventType = event.eventType,
                eventAction = event.eventAction,
                objectId = objectId
            ),
            PIPELINE_WEBHOOK_EVENT_TYPE to event.eventType.value,
            PIPELINE_START_WEBHOOK_USER_ID to event.triggerUser,
            PIPELINE_WEBHOOK_NOTE_COMMENT to body.getHookField(TAPD_KEY_DESCRIPTION),
            // 用于 StartType.transform 与 WebhookInfo 展示，让构建历史正确识别为 TAPD 触发
            PIPELINE_WEBHOOK_TYPE to CodeType.TAPD.name,
            BK_REPO_WEBHOOK_REPO_TYPE to CodeType.TAPD.name,
            // 展示为构建材料的项目别名（TAPD 项目名）
            BK_REPO_WEBHOOK_REPO_ALIAS_NAME to workspaceName,
            BK_CI_MATERIAL_ID to objectId,
            BK_CI_MATERIAL_NAME to title,
            // 让 WebhookInfo.linkUrl 展示 TAPD 工单详情页
            PIPELINE_GIT_EVENT_URL to objectUrl,
            PIPELINE_START_TASK_ID to  element.id!! // 当前触发节点为启动节点
        )
        when (event.eventAction) {
            TapdEventAction.BUG_LINK, TapdEventAction.BUG_UNLINK ->
                TapdEventType.BUG.value to body.getHookField(TAPD_KEY_BUG_ID)

            TapdEventAction.STORY_LINK, TapdEventAction.STORY_UNLINK ->
                TapdEventType.STORY.value to body.getHookField(TAPD_KEY_TARGET_ID)

            else -> null
        }?.let {
            params[CI_TAPD_LINK_TYPE] = it.first
            params[CI_TAPD_LINK_ID] = it.second
        }
        return params
    }

    /**
     * 组装触发事件描述（用于触发事件列表展示）
     */
    fun buildEventDesc(
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        triggerUser: String,
        objectId: String,
        objectUrl: String
    ): String {
        val i18nCode = getEventDescI18nCode(
            eventType = eventType,
            eventAction = eventAction
        )
        return I18Variable(
            code = i18nCode,
            params = listOf(
                objectUrl,
                objectId,
                triggerUser,
                eventType.value,
                eventAction.value
            )
        ).toJsonStr()
    }

    /**
     * 构造工单详情页 URL（仅 STORY / BUG 提供跳转，其它事件类型返回空串）
     */
    fun buildObjectUrl(
        tapdHost: String,
        workspaceId: String,
        objectId: String,
        eventType: TapdEventType
    ): String {
        if (tapdHost.isBlank() || workspaceId.isBlank() || objectId.isBlank()) {
            return ""
        }
        val pattern = when (eventType) {
            TapdEventType.STORY -> TAPD_STORY_URL_PATTERN
            TapdEventType.BUG -> TAPD_BUG_URL_PATTERN
            else -> return ""
        }
        return pattern.format(tapdHost.trimEnd('/'), workspaceId, objectId)
    }

    /**
     * 从 TAPD referer 中提取 host（保留 scheme 与端口）
     */
    fun extractHost(referer: String?): String {
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

    private fun getTitle(
        eventType: TapdEventType,
        body: Map<String, Any?>,
        update: Boolean
    ): String {
        return when (eventType) {
            TapdEventType.STORY -> {
                body.getHookField(TAPD_KEY_NAME, update).ifBlank {
                    body.getHookField(TAPD_KEY_TITLE)
                }
            }
            TapdEventType.BUG -> {
                body.getHookField(TAPD_KEY_TITLE, update).ifBlank {
                    body.getHookField(TAPD_KEY_TITLE)
                }
            }
            else -> ""
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getEventDescI18nCode(
        eventType: TapdEventType,
        eventAction: TapdEventAction
    ): String = when (eventType) {
        TapdEventType.STORY -> when (eventAction) {
            TapdEventAction.CREATE -> BK_TAPD_STORY_CREATE_EVENT_DESC
            TapdEventAction.UPDATE -> BK_TAPD_STORY_UPDATE_EVENT_DESC
            TapdEventAction.DELETE -> BK_TAPD_STORY_DELETE_EVENT_DESC
            TapdEventAction.ADD_COMMENT -> BK_TAPD_STORY_ADD_COMMENT_EVENT_DESC
            TapdEventAction.UPDATE_COMMENT -> BK_TAPD_STORY_UPDATE_COMMENT_EVENT_DESC
            TapdEventAction.DELETE_COMMENT -> BK_TAPD_STORY_DELETE_COMMENT_EVENT_DESC
            TapdEventAction.STATUS_CHANGE -> BK_TAPD_STORY_STATUS_CHANGE_EVENT_DESC
            TapdEventAction.STORY_LINK -> BK_TAPD_STORY_LINK_EVENT_DESC
            TapdEventAction.STORY_UNLINK -> BK_TAPD_STORY_UNLINK_EVENT_DESC
            TapdEventAction.BUG_LINK -> BK_TAPD_STORY_BUG_LINK_EVENT_DESC
            TapdEventAction.BUG_UNLINK -> BK_TAPD_STORY_BUG_UNLINK_EVENT_DESC
            else -> ""
        }

        TapdEventType.BUG -> when (eventAction) {
            TapdEventAction.CREATE -> BK_TAPD_BUG_CREATE_EVENT_DESC
            TapdEventAction.UPDATE -> BK_TAPD_BUG_UPDATE_EVENT_DESC
            TapdEventAction.DELETE -> BK_TAPD_BUG_DELETE_EVENT_DESC
            TapdEventAction.ADD_COMMENT -> BK_TAPD_BUG_ADD_COMMENT_EVENT_DESC
            TapdEventAction.UPDATE_COMMENT -> BK_TAPD_BUG_UPDATE_COMMENT_EVENT_DESC
            TapdEventAction.DELETE_COMMENT -> BK_TAPD_BUG_DELETE_COMMENT_EVENT_DESC
            TapdEventAction.STATUS_CHANGE -> BK_TAPD_BUG_STATUS_CHANGE_EVENT_DESC
            else -> ""
        }

        else -> ""
    }.ifBlank { BK_TAPD_GENERIC_EVENT_DESC }
}

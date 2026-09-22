/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */
package com.tencent.devops.notify.pojo.wework

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 企业微信模板卡片。按审核参数复杂度选择 button_interaction / multiple_interaction。
 * 文档：消息推送 template_card / 应用消息模板卡片。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(title = "企业微信模板卡片")
data class WeworkTemplateCard(
    @JsonProperty("card_type")
    val cardType: String = "button_interaction",
    val source: WeworkTemplateCardSource? = WeworkTemplateCardSource(desc = "蓝盾流水线", descColor = 3),
    @JsonProperty("main_title")
    val mainTitle: WeworkTemplateCardMainTitle,
    @JsonProperty("quote_area")
    val quoteArea: WeworkTemplateCardQuoteArea? = null,
    @JsonProperty("sub_title_text")
    val subTitleText: String? = null,
    @JsonProperty("horizontal_content_list")
    val horizontalContentList: List<WeworkTemplateCardHorizontalContent>? = null,
    @JsonProperty("jump_list")
    val jumpList: List<WeworkTemplateCardJump>? = null,
    @JsonProperty("card_action")
    val cardAction: WeworkTemplateCardAction? = null,
    @JsonProperty("button_selection")
    val buttonSelection: WeworkTemplateCardButtonSelection? = null,
    @JsonProperty("select_list")
    val selectList: List<WeworkTemplateCardSelect>? = null,
    @JsonProperty("submit_button")
    val submitButton: WeworkTemplateCardSubmitButton? = null,
    @JsonProperty("button_list")
    val buttonList: List<WeworkTemplateCardButton>? = null,
    @JsonProperty("task_id")
    val taskId: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardSource(
    val desc: String? = null,
    @JsonProperty("desc_color")
    val descColor: Int? = null,
    @JsonProperty("icon_url")
    val iconUrl: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardMainTitle(
    val title: String,
    val desc: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardQuoteArea(
    val title: String? = null,
    @JsonProperty("quote_text")
    val quoteText: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardHorizontalContent(
    val keyname: String,
    val value: String,
    val type: Int? = null,
    val url: String? = null,
    val userid: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardJump(
    val type: Int = 1,
    val title: String,
    val url: String
)

/**
 * type: 0=回调点击, 1=跳转 URL
 * style: 1=强调蓝, 2=灰, 3=蓝框, 4=红框（以企微实际渲染为准）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardButton(
    val text: String,
    val style: Int? = 2,
    val type: Int = 0,
    val key: String? = null,
    val url: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardAction(
    val type: Int = 1,
    val url: String,
    val title: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardOption(
    val id: String,
    val text: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardButtonSelection(
    @JsonProperty("question_key")
    val questionKey: String,
    val title: String,
    @JsonProperty("option_list")
    val optionList: List<WeworkTemplateCardOption>,
    @JsonProperty("selected_id")
    val selectedId: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardSelect(
    @JsonProperty("question_key")
    val questionKey: String,
    val title: String,
    @JsonProperty("option_list")
    val optionList: List<WeworkTemplateCardOption>,
    @JsonProperty("selected_id")
    val selectedId: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardSubmitButton(
    val text: String,
    val key: String
)

/**
 * 按接收人拆分的审核卡片发送计划。每人独立 task_id，发送失败只降级该人。
 */
data class WeworkReviewCardSendPlan(
    val receiverCards: Map<String, WeworkTemplateCard>,
    val fallbackText: String
)

object WeworkReviewCardConst {
    const val REVIEW_TYPE_ATOM = "ATOM"
    const val REVIEW_TYPE_STAGE = "STAGE"
    const val ACTION_AGREE = "agree"
    const val ACTION_REJECT = "reject"
    const val ACTION_APPROVE = "approve"
    const val ACTION_APPROVE_WITH_PARAMS = "approve_with_params"
    const val ACTION_MODIFY = "modify"
    const val CARD_TYPE_BUTTON = "button_interaction"
    const val CARD_TYPE_MULTIPLE = "multiple_interaction"
    const val MAX_DROPDOWN = 3
    const val MAX_OPTION = 10
    const val MAX_HLIST = 6
    const val BUTTON_KEY_PREFIX = "BKCI_REVIEW"
    const val REDIS_KEY_PREFIX = "notify:wework:review:card:"
    const val DEFAULT_REJECT_SUGGEST = "企业微信卡片一键驳回"
    /** 卡片任务缓存 7 天，覆盖常见审核超时 */
    const val REDIS_TTL_SECONDS = 7 * 24 * 3600L
    private val TASK_ID_UNSAFE = Regex("[^A-Za-z0-9_\\-@]")

    fun buttonKey(action: String, taskId: String) = "$BUTTON_KEY_PREFIX|$action|$taskId"

    fun parseButtonKey(eventKey: String): Pair<String, String>? {
        val parts = eventKey.split("|")
        if (parts.size != 3 || parts[0] != BUTTON_KEY_PREFIX) return null
        val action = when (parts[1]) {
            ACTION_APPROVE, ACTION_APPROVE_WITH_PARAMS -> ACTION_AGREE
            else -> parts[1]
        }
        return action to parts[2]
    }

    fun weworkUserId(raw: String): String = raw.substringBefore("@").trim()

    /**
     * 企微 task_id：同应用内不可重复，最长 128，仅数字字母和 _-@。
     * 设计稿的 review_{project}_{build}_{stage} 在多人审核时会冲突，因此附加接收人与盐。
     */
    fun buildTaskId(
        projectId: String,
        buildId: String,
        scopeId: String,
        receiver: String,
        salt: String
    ): String {
        val parts = listOf("review", projectId, buildId, scopeId, receiver, salt).map { sanitizeTaskIdPart(it) }
        return parts.joinToString("_").take(128)
    }

    fun appendUrlAction(url: String, action: String): String {
        if (url.isBlank() || url.contains("action=")) return url
        return if (url.contains("?")) "$url&action=$action" else "$url?action=$action"
    }

    private fun sanitizeTaskIdPart(value: String): String {
        return TASK_ID_UNSAFE.replace(value, "-").ifBlank { "x" }.take(32)
    }
}

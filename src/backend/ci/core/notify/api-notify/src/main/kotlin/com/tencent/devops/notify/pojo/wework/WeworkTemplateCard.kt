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
 * 企业微信模板卡片（button_interaction），用于审核同意/拒绝与双端详情跳转。
 * 文档：消息推送 template_card / 应用消息模板卡片。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(title = "企业微信模板卡片")
data class WeworkTemplateCard(
    @JsonProperty("card_type")
    val cardType: String = "button_interaction",
    val source: WeworkTemplateCardSource? = WeworkTemplateCardSource(desc = "蓝盾"),
    @JsonProperty("main_title")
    val mainTitle: WeworkTemplateCardMainTitle,
    @JsonProperty("horizontal_content_list")
    val horizontalContentList: List<WeworkTemplateCardHorizontalContent>? = null,
    @JsonProperty("button_list")
    val buttonList: List<WeworkTemplateCardButton>,
    @JsonProperty("task_id")
    val taskId: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardSource(
    val desc: String? = null,
    @JsonProperty("desc_color")
    val descColor: Int? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardMainTitle(
    val title: String,
    val desc: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardHorizontalContent(
    val keyname: String,
    val value: String,
    val type: Int? = null,
    val url: String? = null
)

/**
 * type: 0=回调点击, 1=跳转 URL
 * style: 1=绿/强调, 2=灰, 3=红, 4=蓝等（以企微实际渲染为准）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeworkTemplateCardButton(
    val text: String,
    val style: Int? = 2,
    val type: Int = 0,
    val key: String? = null,
    val url: String? = null
)

object WeworkReviewCardConst {
    const val REVIEW_TYPE_ATOM = "ATOM"
    const val REVIEW_TYPE_STAGE = "STAGE"
    const val ACTION_AGREE = "agree"
    const val ACTION_REJECT = "reject"
    const val BUTTON_KEY_PREFIX = "BKCI_REVIEW"
    const val REDIS_KEY_PREFIX = "notify:wework:review:card:"
    const val DEFAULT_REJECT_SUGGEST = "企业微信卡片一键驳回"
    /** 卡片任务缓存 7 天，覆盖常见审核超时 */
    const val REDIS_TTL_SECONDS = 7 * 24 * 3600L

    fun buttonKey(action: String, taskId: String) = "$BUTTON_KEY_PREFIX|$action|$taskId"

    fun parseButtonKey(eventKey: String): Pair<String, String>? {
        val parts = eventKey.split("|")
        if (parts.size != 3 || parts[0] != BUTTON_KEY_PREFIX) return null
        return parts[1] to parts[2]
    }
}

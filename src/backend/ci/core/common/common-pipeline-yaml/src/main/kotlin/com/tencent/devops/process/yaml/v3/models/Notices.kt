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

package com.tencent.devops.process.yaml.v3.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.pojo.transfer.IfType
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.utils.NotifyTemplateUtils
import com.tencent.devops.process.yaml.v3.enums.ContentFormat
import io.swagger.v3.oas.annotations.media.Schema

/**
 * model Stream 通知类型基类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
interface Notices {
    fun toSubscription() = Subscription()

    fun checkNotifyForSuccess() = false

    fun checkNotifyForFail() = false
}

/**
 * model Stream Yaml基本通知
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitNotices(
    val type: String,
    val receivers: Set<String>?,
    val title: String?,
    val content: String?,
    val ccs: Set<String>?,
    @get:Schema(title = "if")
    @JsonProperty("if")
    val ifField: String?,
    @get:Schema(title = "chat-id")
    @JsonProperty("chat-id")
    val chatId: Set<String>?
) : Notices {

    constructor(subscription: Subscription, ifField: String?) : this(
        type = subscription.types.map { PacNotices.toNotifyType(it) }.toMutableList().apply { sort() }.also {
            if (subscription.wechatGroupFlag) it.add(NotifyType.RTX_GROUP.yamlText)
        }.first(),
        receivers = subscription.users.split(",").ifEmpty { null }?.toSet(),
        title = null,
        content = subscription.content.ifEmpty { null },
        ccs = null,
        ifField = ifField,
        chatId = subscription.wechatGroup.split(",").ifEmpty { null }?.toSet()
    )

    override fun toSubscription() = Subscription(
        types = setOf(PacNotices.toPipelineSubscriptionType(type)),
        groups = emptySet(),
        users = receivers?.joinToString(",") ?: "",
        wechatGroupFlag = type.contains(NotifyType.RTX_GROUP.yamlText),
        wechatGroup = chatId?.joinToString(",") ?: "",
        wechatGroupMarkdownFlag = false,
        detailFlag = false,
        content = content ?: ""
    )

    override fun checkNotifyForSuccess(): Boolean {
        return ifField == null || ifField == IfType.SUCCESS.name || ifField == IfType.ALWAYS.name
    }

    override fun checkNotifyForFail(): Boolean {
        return ifField == null || ifField == IfType.FAILURE.name || ifField == IfType.ALWAYS.name
    }
}

/**
 * pac 通知
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PacNotices(
    @get:Schema(title = "if")
    @JsonProperty("if")
    val ifField: String?,
    val type: Any,
    val receivers: List<String>?,
    val groups: List<String>?,
    val content: String?,
    @get:Schema(title = "chat-id")
    @JsonProperty("chat-id")
    val chatId: List<String>?,
    @get:Schema(title = "content-format")
    @JsonProperty("content-format")
    val notifyMarkdown: String?,
    @get:Schema(title = "attach-build-url")
    @JsonProperty("attach-build-url")
    val notifyDetail: Boolean?
) : Notices {

    constructor(subscription: Subscription, ifField: String?) : this(
        type = subscription.types.map { toNotifyType(it) }.toMutableList().apply { sort() }.also {
            if (subscription.wechatGroupFlag) it.add(NotifyType.RTX_GROUP.yamlText)
        }.toSet(),
        receivers = subscription.users.ifBlank { null }?.split(",")?.toSet()?.toList(),
        groups = subscription.groups.ifEmpty { null }?.sorted(),
        content = subscription.content.ifEmpty { null },
        ifField = ifField,
        chatId = subscription.wechatGroup.ifBlank { null }?.split(",")?.toSet()?.toList(),
        notifyMarkdown = ContentFormat.parse(subscription.wechatGroupMarkdownFlag)
            .nullIfDefault(ContentFormat.TEXT)?.text,
        notifyDetail = subscription.detailFlag.nullIfDefault(false)
    )

    companion object {
        fun toPipelineSubscriptionType(type: String) = when (type) {
            NotifyType.EMAIL.yamlText -> PipelineSubscriptionType.EMAIL
            NotifyType.RTX_CUSTOM.yamlText -> PipelineSubscriptionType.RTX
            NotifyType.SMS.yamlText -> PipelineSubscriptionType.SMS
            NotifyType.VOICE.yamlText -> PipelineSubscriptionType.VOICE
            else -> PipelineSubscriptionType.RTX
        }

        fun toNotifyType(type: PipelineSubscriptionType) = when (type) {
            PipelineSubscriptionType.EMAIL -> NotifyType.EMAIL.yamlText
            PipelineSubscriptionType.RTX -> NotifyType.RTX_CUSTOM.yamlText
            PipelineSubscriptionType.SMS -> NotifyType.SMS.yamlText
            PipelineSubscriptionType.VOICE -> NotifyType.VOICE.yamlText
            else -> NotifyType.RTX_GROUP.yamlText
        }
    }

    override fun toSubscription(): Subscription {
        val parseType = parseType()
        return Subscription(
            types = parseType.map { toPipelineSubscriptionType(it) }.toSet(),
            groups = groups?.toSet() ?: emptySet(),
            users = receivers?.joinToString(",") ?: "\${{ci.actor}}",
            wechatGroupFlag = parseType.contains(NotifyType.RTX_GROUP.yamlText),
            wechatGroup = chatId?.joinToString(",") ?: "",
            wechatGroupMarkdownFlag = notifyMarkdown == ContentFormat.MARKDOWN.text,
            detailFlag = notifyDetail ?: false,
            content = content ?: defaultContent()
        )
    }

    private fun defaultContent() = when {
        checkNotifyForSuccess() -> NotifyTemplateUtils.getCommonShutdownSuccessContent()
        else -> NotifyTemplateUtils.getCommonShutdownFailureContent()
    }

    override fun checkNotifyForSuccess(): Boolean {
        return ifField == null || ifField == IfType.SUCCESS.name || ifField == IfType.ALWAYS.name
    }

    override fun checkNotifyForFail(): Boolean {
        return ifField == null || ifField == IfType.FAILURE.name || ifField == IfType.ALWAYS.name
    }

    private fun parseType(): List<String> = when (type) {
        is String -> listOf(type)
        is List<*> -> type.map { it.toString() }
        else -> emptyList()
    }
}

enum class NotifyType(val yamlText: String) {
    // 企业微信客服
    SMS("sms"),

    RTX_CUSTOM("wework-message"),

    // 邮件
    EMAIL("email"),

    // 邮件
    VOICE("voice"),

    // 企业微信群
    RTX_GROUP("wework-chat");
}

/**
 * model Stream 质量红线通知
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class GateNotices(
    val type: String,
    val receivers: Set<String>?
) : Notices

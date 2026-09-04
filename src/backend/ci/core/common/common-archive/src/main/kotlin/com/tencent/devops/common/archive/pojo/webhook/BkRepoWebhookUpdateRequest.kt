package com.tencent.devops.common.archive.pojo.webhook

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 更新 bkrepo webhook 请求（对应 bkrepo UpdateWebHookRequest）
 */
@Schema(title = "更新 bkrepo webhook 请求")
data class BkRepoWebhookUpdateRequest(
    @get:Schema(title = "webhook ID")
    val id: String,
    @get:Schema(title = "回调地址")
    val url: String? = null,
    @get:Schema(title = "自定义请求头")
    val headers: Map<String, String>? = null,
    @get:Schema(title = "触发事件列表")
    val triggers: List<BkRepoEventType>? = null,
    @get:Schema(title = "事件资源key正则模式")
    val resourceKeyPattern: String? = null
)

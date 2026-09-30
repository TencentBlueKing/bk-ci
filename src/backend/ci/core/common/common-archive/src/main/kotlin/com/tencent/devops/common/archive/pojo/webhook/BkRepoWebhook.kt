package com.tencent.devops.common.archive.pojo.webhook

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

/**
 * bkrepo webhook 信息（对应 bkrepo WebHook 响应）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "bkrepo webhook 信息")
data class BkRepoWebhook(
    @get:Schema(title = "webhook ID")
    val id: String,
    @get:Schema(title = "回调地址")
    val url: String,
    @get:Schema(title = "自定义请求头")
    val headers: Map<String, String>? = null,
    @get:Schema(title = "触发事件列表")
    val triggers: List<BkRepoEventType> = emptyList(),
    @get:Schema(title = "关联对象类型")
    val associationType: BkRepoAssociationType? = null,
    @get:Schema(title = "关联对象ID")
    val associationId: String? = null,
    @get:Schema(title = "事件资源key正则模式")
    val resourceKeyPattern: String? = null
)

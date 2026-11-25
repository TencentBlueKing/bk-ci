package com.tencent.devops.common.archive.pojo.webhook

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 创建 bkrepo webhook 请求（对应 bkrepo CreateWebHookRequest）
 */
@Schema(title = "创建 bkrepo webhook 请求")
data class BkRepoWebhookCreateRequest(
    @get:Schema(title = "回调地址")
    val url: String,
    @get:Schema(title = "自定义请求头")
    val headers: Map<String, String> = emptyMap(),
    @get:Schema(title = "触发事件列表")
    val triggers: List<BkRepoEventType>,
    @get:Schema(title = "关联对象类型")
    val associationType: BkRepoAssociationType,
    @get:Schema(title = "关联对象ID")
    val associationId: String = "",
    @get:Schema(title = "事件资源key正则模式")
    val resourceKeyPattern: String? = null
)

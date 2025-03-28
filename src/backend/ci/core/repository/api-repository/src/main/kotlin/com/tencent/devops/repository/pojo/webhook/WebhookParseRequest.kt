package com.tencent.devops.repository.pojo.webhook

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "webhook解析请求")
data class WebhookParseRequest(
    @get:Schema(title = "请求ID", required = true)
    val requestId: String,
    @get:Schema(title = "请求头", required = true)
    val headers: Map<String, String>? = null,
    @get:Schema(title = "请求参数", required = true)
    val queryParams: Map<String, String>? = null,
    @get:Schema(title = "请求体", required = true)
    val body: String
)

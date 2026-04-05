package com.tencent.devops.process.pojo.trigger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店事件触发启动请求")
data class MarketEventStartRequest(
    @get:Schema(title = "触发器插件匹配参数", required = true)
    val eventBody: GenericWebhookEventBody,
    @get:Schema(title = "流水线启动参数")
    val startParams: Map<String, String>? = null
)

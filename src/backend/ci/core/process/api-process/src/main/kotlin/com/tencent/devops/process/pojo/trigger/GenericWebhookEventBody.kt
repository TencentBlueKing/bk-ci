package com.tencent.devops.process.pojo.trigger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "通用webhook事件请求体")
data class GenericWebhookEventBody(
    @get:Schema(description = "请求头")
    val headers: Map<String, String>? = null,
    @get:Schema(description = "请求参数")
    val queryParams: Map<String, String>? = null,
    @get:Schema(description = "请求体")
    val body: Map<String, String>? = null
) : TriggerEventBody {
    companion object {
        const val classType = "generic"
    }
}

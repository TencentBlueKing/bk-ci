package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.scm.api.pojo.webhook.Webhook
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "代码库webhook事件请求体")
data class ScmWebhookEventBody(
    @get:Schema(description = "请求头")
    val headers: Map<String, String>? = null,
    @get:Schema(description = "请求参数")
    val queryParams: Map<String, String>? = null,
    @get:Schema(description = "webhook请求体")
    val webhook: Webhook
) : TriggerEventBody {
    companion object {
        const val classType = "scm"
    }
}

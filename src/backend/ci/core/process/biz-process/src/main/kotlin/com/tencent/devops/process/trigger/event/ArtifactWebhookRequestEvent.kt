package com.tencent.devops.process.trigger.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品到达 Webhook 请求事件
 *
 * 参考 [ScmWebhookRequestEvent]：外部接口只把原始请求（headers/queryParams/body）投递到 MQ，
 * 具体解析（多态事件反序列化）延迟到消费端 [com.tencent.devops.process.trigger.artifact.ArtifactWebhookRequestService]。
 */
@Event(StreamBinding.ARTIFACT_WEBHOOK_REQUEST_EVENT)
@Schema(title = "制品到达 webhook 请求事件")
data class ArtifactWebhookRequestEvent(
    @get:Schema(title = "bkrepo 事件类型（X-BKREPO-EVENT）")
    val eventType: String,
    @get:Schema(title = "制品 hook 请求体")
    val request: WebhookRequest
) : IEvent()

package com.tencent.devops.process.trigger.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import io.swagger.v3.oas.annotations.media.Schema

@Event(StreamBinding.GENERIC_WEBHOOK_REQUEST_EVENT)
data class GenericWebhookRequestEvent(
    @get:Schema(title = "事件标识")
    val eventCode: String,
    @get:Schema(title = "hook请求体")
    val request: WebhookRequest
) : IEvent()

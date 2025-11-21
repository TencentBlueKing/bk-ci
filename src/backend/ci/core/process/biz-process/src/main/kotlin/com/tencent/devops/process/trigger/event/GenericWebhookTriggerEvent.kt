package com.tencent.devops.process.trigger.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import io.swagger.v3.oas.annotations.media.Schema

@Event(StreamBinding.GENERIC_WEBHOOK_REQUEST_EVENT)
data class GenericWebhookTriggerEvent(
    @get:Schema(description = "项目ID")
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线版本", required = false)
    val version: Int?,
    @get:Schema(description = "事件编码")
    val eventCode: String,
    @get:Schema(description = "事件ID")
    val eventId: Long,
    @get:Schema(title = "事件源", required = true)
    val eventSource: String,
    @get:Schema(title = "事件请求时间", required = true)
    val requestTime: Long
) : IEvent()

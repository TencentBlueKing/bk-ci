package com.tencent.devops.process.trigger.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import io.swagger.v3.oas.annotations.media.Schema

@Event(StreamBinding.REMOTEDEV_WEBHOOK_REQUEST_EVENT)
data class RemoteDevWebhookRequestEvent(
    @get:Schema(description = "用户ID")
    val userId: String,
    @get:Schema(description = "项目ID")
    val projectId: String,
    @get:Schema(description = "云桌面实例ID")
    val workspaceName: String,
    @get:Schema(description = "云桌面IP")
    val cdsIp: String,
    @get:Schema(description = "事件编码")
    val eventCode: String,
    @get:Schema(description = "请求体")
    val body: String
) : IEvent()

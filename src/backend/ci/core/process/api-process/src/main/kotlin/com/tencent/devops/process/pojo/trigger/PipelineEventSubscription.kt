package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.common.pipeline.enums.ChannelCode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线事件订阅表")
data class PipelineEventSubscription(
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "流水线Id")
    val pipelineId: String,
    @get:Schema(title = "插件ID")
    val taskId: String,
    @get:Schema(title = "事件编码")
    val eventCode: String,
    @get:Schema(title = "事件源")
    val eventSource: String,
    @get:Schema(title = "事件类型")
    val eventType: String,
    @get:Schema(title = "流水线创建渠道")
    val channelCode: ChannelCode
)

package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.common.pipeline.enums.ChannelCode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线订阅者")
data class PipelineEventSubscriber(
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "流水线Id")
    val pipelineId: String,
    @get:Schema(title = "流水线创建渠道")
    val channelCode: ChannelCode
)

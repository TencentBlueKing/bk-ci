package com.tencent.devops.process.pojo.webhook

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion

data class WebhookStartPipelineRequest(
    val pipelineInfo: PipelineInfo,
    val startType: StartType,
    val pipelineParamMap: MutableMap<String, BuildParameters>,
    val channelCode: ChannelCode,
    val resource: PipelineResourceVersion,
    val signPipelineVersion: Int? = null,
    val frequencyLimit: Boolean = false
)

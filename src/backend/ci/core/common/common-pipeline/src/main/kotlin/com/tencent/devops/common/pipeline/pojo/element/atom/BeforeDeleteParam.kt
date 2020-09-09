package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.enums.ChannelCode

data class BeforeDeleteParam(
    val userId: String,
    val projectId: String,
    val pipelineId: String,
    val channelCode: ChannelCode = ChannelCode.BS
)
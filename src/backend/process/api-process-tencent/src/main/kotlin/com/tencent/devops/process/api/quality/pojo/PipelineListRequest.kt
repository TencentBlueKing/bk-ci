package com.tencent.devops.process.api.quality.pojo

data class PipelineListRequest(
    val pipelineId: Collection<String>,
    val templateId: Collection<String>
)
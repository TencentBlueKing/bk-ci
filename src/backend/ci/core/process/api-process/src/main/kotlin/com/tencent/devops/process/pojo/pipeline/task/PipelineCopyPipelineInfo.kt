package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源关联的流水线")
data class PipelineCopyPipelineInfo(
    @get:Schema(description = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(description = "流水线名称", required = true)
    val pipelineName: String
)

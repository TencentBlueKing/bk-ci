package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务创建请求")
data class PipelineCopyTaskCreateRequest(
    @get:Schema(description = "批量复制的流水线ID列表", required = true)
    val pipelineIds: List<String>
)

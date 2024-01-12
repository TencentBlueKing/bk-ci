package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "批量删除流水线")
data class BatchDeletePipeline(
    @Schema(description = "项目ID")
    val projectId: String,
    @Schema(description = "流水线ID列表")
    val pipelineIds: List<String>
)

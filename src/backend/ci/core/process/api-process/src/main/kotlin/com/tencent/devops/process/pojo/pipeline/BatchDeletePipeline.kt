package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量删除流水线")
data class BatchDeletePipeline(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "流水线ID列表")
    val pipelineIds: List<String>
)

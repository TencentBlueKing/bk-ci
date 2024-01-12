package com.tencent.devops.process.pojo.classify

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "流水线组批量添加")
data class PipelineViewBulkAdd(
    @Schema(name = "流水线ID列表")
    val pipelineIds: List<String>,
    @Schema(name = "视图ID列表")
    val viewIds: List<String>
)

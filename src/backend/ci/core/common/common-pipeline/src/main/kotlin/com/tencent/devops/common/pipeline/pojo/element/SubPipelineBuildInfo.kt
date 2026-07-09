package com.tencent.devops.common.pipeline.pojo.element

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "子流水线构建信息（运行态）")
data class SubPipelineBuildInfo(
    @get:Schema(title = "子流水线所属项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "子流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "子流水线构建ID", required = true)
    val buildId: String
)

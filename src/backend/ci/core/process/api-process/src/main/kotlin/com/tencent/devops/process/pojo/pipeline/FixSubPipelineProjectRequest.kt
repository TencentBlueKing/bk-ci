package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "修复子流水线插件项目ID请求")
data class FixSubPipelineProjectRequest(
    @get:Schema(title = "流水线ID列表", required = true)
    val pipelineIds: List<String>,
    @get:Schema(title = "子流水线旧项目ID", required = true)
    val sourceSubProjectId: String,
    @get:Schema(title = "子流水线新项目ID", required = true)
    val targetSubProjectId: String
)

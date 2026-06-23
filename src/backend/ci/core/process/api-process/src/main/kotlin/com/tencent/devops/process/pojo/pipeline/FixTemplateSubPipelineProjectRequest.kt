package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "修复模板子流水线插件项目ID请求")
data class FixTemplateSubPipelineProjectRequest(
    @get:Schema(title = "模板ID列表", required = true)
    val templateIds: List<String>,
    @get:Schema(title = "子流水线旧项目ID", required = true)
    val sourceSubProjectId: String,
    @get:Schema(title = "子流水线新项目ID", required = true)
    val targetSubProjectId: String
)

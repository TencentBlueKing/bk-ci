package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板详情查询条件")
data class PipelineTemplateDetailsSearchReq(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String? = null,
    @get:Schema(title = "模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "模板版本", required = true)
    val version: Long?
)

package com.tencent.devops.process.pojo.template

import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板列表-简化")
data class PipelineTemplateListSimpleResponse(
    @get:Schema(title = "模板ID", required = true)
    val id: String,
    @get:Schema(title = "模板名称", required = true)
    val name: String,
    @get:Schema(title = "模板类型", required = true)
    val type: PipelineTemplateType
)

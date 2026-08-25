package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板自定义创建请求体")
data class PipelineTemplateCustomCreateReq(
    @get:Schema(title = "类型", required = true)
    val type: PipelineTemplateType,
    @get:Schema(title = "模板名称", required = true)
    val name: String,
    @get:Schema(title = "简介", required = false)
    val desc: String?
) : PipelineTemplateVersionReq

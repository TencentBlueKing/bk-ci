package com.tencent.devops.process.pojo.template

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板信息")
class TemplatePipelineInfo(
    @get:Schema(title = "模板id", required = false)
    val templateId: String? = null,
    @get:Schema(title = "版本名称", required = false)
    val versionName: String? = null,
    @get:Schema(title = "版本", required = false)
    val version: Long? = null,
    @get:Schema(title = "流水线id", required = false)
    val pipelineId: String
)

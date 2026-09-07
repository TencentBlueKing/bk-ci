package com.tencent.devops.process.pojo.template

import com.tencent.devops.process.pojo.PipelineTemplateVersionSimple
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDraftStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线模板草稿状态结果")
data class PipelineTemplateDraftStatusResult(
    @get:Schema(description = "草稿状态", required = true)
    val status: PipelineDraftStatus,
    @get:Schema(description = "草稿版本信息", required = false)
    val draft: PipelineTemplateVersionSimple? = null,
    @get:Schema(description = "正式版本信息", required = false)
    val release: PipelineTemplateVersionSimple? = null
)

package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模型-构建版本差异")
data class BuildVersionDiffInfo(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "构建历史", required = true)
    val pipelineId: String,
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "模板类型，PIPELINE/STAGE/JOB/STEP/VARIABLE", required = true)
    val templateType: PipelineTemplateType,
    @get:Schema(title = "模板名称", required = true)
    val templateName: String,
    @get:Schema(title = "模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "模板版本名称-分支名/tag/latest", required = true)
    val templateVersionName: String,
    @get:Schema(title = "上一个版本号", required = true)
    val prevTemplateVersion: Long,
    @get:Schema(title = "当前版本号", required = true)
    val currTemplateVersion: Long,
    @get:Schema(title = "上一次版本名/上一次commit")
    val prevTemplateVersionRef: String,
    @get:Schema(title = "当前版本号/当前commit")
    val currTemplateVersionRef: String
)

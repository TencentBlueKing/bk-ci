package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线模版复制资源属性")
data class PipelineTemplateCopyResourceProp(
    @get:Schema(description = "模版版本映射关系")
    val versionMappings: List<TemplateVersionMapping>
) : PipelineCopyResourceProp {
    companion object {
        const val classType = "pipelineTemplate"
    }
}

@Schema(description = "模版版本映射")
data class TemplateVersionMapping(
    @get:Schema(description = "源版本")
    val sourceVersion: Long,
    @get:Schema(description = "源版本名")
    val sourceVersionName: String,
    @get:Schema(description = "目标版本")
    val targetVersion: Long,
    @get:Schema(description = "目标版本名")
    val targetVersionName: String
)

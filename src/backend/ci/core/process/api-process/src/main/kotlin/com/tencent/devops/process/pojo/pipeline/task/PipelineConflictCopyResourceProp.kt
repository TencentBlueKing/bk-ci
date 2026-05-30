package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线冲突复制资源属性")
data class PipelineConflictCopyResourceProp(
    val idConflict: PipelineConflictInfo? = null,
    val nameConflict: PipelineConflictInfo? = null
) : PipelineCopyResourceProp {
    companion object {
        const val classType = "pipelineConflict"
    }
}

@Schema(description = "流水线冲突信息")
data class PipelineConflictInfo(
    @get:Schema(description = "流水线ID")
    val pipelineId: String,
    @get:Schema(description = "流水线名称")
    val pipelineName: String,
    @get:Schema(description = "创建人")
    val creator: String
)

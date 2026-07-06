package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量复制任务参数")
data class PipelineBatchCopyTaskParam(
    @get:Schema(description = "目标项目ID", required = true)
    val targetProjectId: String,
    @get:Schema(description = "流水线处理策略", required = true)
    val pipelineCopyStrategy: PipelineCopyStrategy,
    @get:Schema(description = "流水线标签复制策略")
    val pipelineLabelCopyStrategy: PipelineCopyStrategy? = null,
    @get:Schema(description = "流水线组复制策略")
    val pipelineGroupCopyStrategy: PipelineCopyStrategy? = null,
) : PipelineBatchTaskParam {
    companion object {
        const val classType = "copy"
    }
}

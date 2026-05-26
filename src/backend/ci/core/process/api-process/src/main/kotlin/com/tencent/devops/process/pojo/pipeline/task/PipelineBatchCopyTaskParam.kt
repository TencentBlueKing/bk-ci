package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量复制任务参数")
data class PipelineBatchCopyTaskParam(
    @get:Schema(description = "目标项目ID", required = true)
    val targetProjectId: String,
    @get:Schema(description = "流水线处理策略", required = true)
    val pipelineCopyStrategy: PipelineCopyStrategy
) : PipelineBatchTaskParam {
    companion object {
        const val classType = "copy"
    }
}

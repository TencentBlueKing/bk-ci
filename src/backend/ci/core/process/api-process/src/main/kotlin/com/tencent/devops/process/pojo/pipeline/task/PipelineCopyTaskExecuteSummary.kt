package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务执行汇总")
data class PipelineCopyTaskExecuteSummary(
    @get:Schema(description = "流水线数量", required = true)
    val pipelineCount: Int,
    @get:Schema(description = "资源需要补齐数量", required = true)
    val needCompletionCount: Int,
    @get:Schema(description = "资源需要迁移数量", required = true)
    val needTransferCount: Int,
    @get:Schema(description = "自动完成数量", required = true)
    val autoFinishCount: Int
)

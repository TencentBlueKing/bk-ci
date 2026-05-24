package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务明细状态汇总项")
data class PipelineBatchTaskDetailStatusSummary(
    @get:Schema(description = "明细状态", required = true)
    val status: PipelineBatchTaskDetailStatus,
    @get:Schema(description = "数量", required = true)
    val count: Long
)

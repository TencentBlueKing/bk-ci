package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务状态汇总项")
data class PipelineBatchTaskStatusSummary(
    @get:Schema(description = "任务状态", required = true)
    val status: PipelineBatchTaskStatus,
    @get:Schema(description = "数量", required = true)
    val count: Long
)

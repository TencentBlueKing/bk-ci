package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务执行进度")
data class PipelineCopyTaskExecuteProgress(
    @get:Schema(description = "任务状态", required = true)
    val status: PipelineBatchTaskStatus,
    @get:Schema(description = "任务总数", required = true)
    val totalCount: Long,
    @get:Schema(description = "已执行数量", required = true)
    val executedCount: Long
)

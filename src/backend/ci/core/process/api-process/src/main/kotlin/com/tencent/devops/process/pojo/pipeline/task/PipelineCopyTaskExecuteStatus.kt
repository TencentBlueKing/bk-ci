package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务执行状态")
data class PipelineCopyTaskExecuteStatus(
    @get:Schema(description = "任务状态", required = true)
    val status: PipelineCopyTaskStatus,
    @get:Schema(description = "任务总数", required = true)
    val totalCount: Long,
    @get:Schema(description = "已执行数量", required = true)
    val executedCount: Long,
    @get:Schema(description = "流水线总数", required = true)
    val pipelineTotalCount: Long,
    @get:Schema(description = "资源需要补齐数量", required = true)
    val needCompletionCount: Long,
    @get:Schema(description = "资源需要迁移数量", required = true)
    val needTransferCount: Long,
    @get:Schema(description = "自动完成数量", required = true)
    val autoFinishCount: Long,
)

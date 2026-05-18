package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "流水线批量任务信息")
data class PipelineBatchTaskInfo(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "任务名称", required = true)
    val taskName: String,
    @get:Schema(description = "任务类型", required = true)
    val taskType: PipelineBatchTaskType,
    @get:Schema(description = "任务参数")
    val taskParam: String?,
    @get:Schema(description = "任务状态", required = true)
    val status: PipelineBatchTaskStatus,
    @get:Schema(description = "总数", required = true)
    val totalCount: Int,
    @get:Schema(description = "成功数", required = true)
    val successCount: Int,
    @get:Schema(description = "失败数", required = true)
    val failedCount: Int,
    @get:Schema(description = "创建人", required = true)
    val creator: String,
    @get:Schema(description = "创建时间")
    val createTime: LocalDateTime?,
    @get:Schema(description = "更新时间")
    val updateTime: LocalDateTime?
)

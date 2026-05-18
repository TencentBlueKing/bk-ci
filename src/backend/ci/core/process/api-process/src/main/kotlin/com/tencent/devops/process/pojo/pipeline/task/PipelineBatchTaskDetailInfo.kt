package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "流水线批量任务明细信息")
data class PipelineBatchTaskDetailInfo(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(description = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(description = "明细状态", required = true)
    val status: PipelineBatchTaskStatus,
    @get:Schema(description = "错误信息")
    val errorMessage: String?,
    @get:Schema(description = "开始时间")
    val startTime: LocalDateTime?,
    @get:Schema(description = "结束时间")
    val endTime: LocalDateTime?,
    @get:Schema(description = "创建时间")
    val createTime: LocalDateTime? = null,
    @get:Schema(description = "更新时间")
    val updateTime: LocalDateTime? = null
)

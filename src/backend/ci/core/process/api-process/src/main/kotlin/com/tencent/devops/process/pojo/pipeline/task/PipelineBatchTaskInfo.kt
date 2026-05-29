package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStep
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务信息")
data class PipelineBatchTaskInfo(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "任务名称")
    val taskName: String?,
    @get:Schema(description = "任务类型", required = true)
    val taskType: PipelineBatchTaskType,
    @get:Schema(description = "任务参数")
    val taskParam: String?,
    @get:Schema(description = "任务汇总信息")
    val taskSummary: String? = null,
    @get:Schema(description = "任务状态", required = true)
    val status: PipelineBatchTaskStatus,
    @get:Schema(description = "当前步骤", required = true)
    val step: PipelineBatchTaskStep,
    @get:Schema(description = "总数", required = true)
    val totalCount: Int,
    @get:Schema(description = "自动添加的子流水线数量", required = true)
    val subPipelineCount: Int = 0,
    @get:Schema(description = "PAC数量", required = true)
    val pacCount: Int = 0,
    @get:Schema(description = "成功数", required = true)
    val successCount: Int,
    @get:Schema(description = "失败数", required = true)
    val failedCount: Int,
    @get:Schema(description = "创建人", required = true)
    val creator: String,
    @get:Schema(description = "创建时间")
    val createTime: Long?,
    @get:Schema(description = "更新时间")
    val updateTime: Long?
)

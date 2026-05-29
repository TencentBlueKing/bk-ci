package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStep
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务更新")
data class PipelineBatchTaskUpdate(
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "任务名称")
    val taskName: String? = null,
    @get:Schema(description = "任务参数")
    val taskParam: String? = null,
    @get:Schema(description = "任务汇总信息")
    val taskSummary: String? = null,
    @get:Schema(description = "任务状态")
    val status: PipelineBatchTaskStatus? = null,
    @get:Schema(description = "自动添加的子流水线数量")
    val subPipelineCount: Int? = null,
    @get:Schema(description = "PAC数量")
    val pacCount: Int? = null,
    @get:Schema(description = "成功数")
    val successCount: Int? = null,
    @get:Schema(description = "失败数")
    val failedCount: Int? = null,
    @get:Schema(description = "当前步骤")
    val step: PipelineBatchTaskStep? = null
)

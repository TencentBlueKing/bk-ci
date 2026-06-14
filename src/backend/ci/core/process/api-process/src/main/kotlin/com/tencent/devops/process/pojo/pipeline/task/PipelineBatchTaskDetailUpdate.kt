package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailErrorType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务明细更新")
data class PipelineBatchTaskDetailUpdate(
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "流水线ID")
    val pipelineId: String,
    @get:Schema(description = "明细状态")
    val status: PipelineBatchTaskDetailStatus? = null,
    @get:Schema(description = "是否修改")
    val change: Boolean? = null,
    @get:Schema(description = "错误类型")
    val errorType: PipelineBatchTaskDetailErrorType? = null,
    @get:Schema(description = "错误信息")
    val errorMessage: PipelineBatchTaskErrorMessage? = null
)

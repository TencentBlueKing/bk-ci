package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务创建请求")
data class PipelineBatchTaskCreateRequest(
    @get:Schema(description = "任务名称")
    val taskName: String? = null,
    @get:Schema(description = "任务类型", required = true)
    val taskType: PipelineBatchTaskType,
    @get:Schema(description = "流水线ID列表", required = true)
    val pipelineIds: List<String>
)

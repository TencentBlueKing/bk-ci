package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务配置请求")
data class PipelineBatchTaskConfigRequest(
    @get:Schema(description = "任务名称", required = true)
    val taskName: String,
    @get:Schema(description = "任务参数")
    val taskParam: PipelineBatchTaskParam? = null
)

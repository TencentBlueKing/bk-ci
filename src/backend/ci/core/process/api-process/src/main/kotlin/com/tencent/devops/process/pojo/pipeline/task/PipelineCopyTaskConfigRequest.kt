package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务配置请求")
data class PipelineCopyTaskConfigRequest(
    @get:Schema(description = "目标项目ID", required = true)
    val targetProjectId: String,
    @get:Schema(description = "任务名称", required = true)
    val taskName: String,
    @get:Schema(description = "流水线ID处理策略", required = true)
    val pipelineCopyStrategy: PipelineCopyStrategy
)

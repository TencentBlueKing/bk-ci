package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务标签汇总")
data class PipelineBatchTaskLabelSummary(
    @get:Schema(description = "开启PAC的明细数量", required = true)
    val pacCount: Long,
    @get:Schema(description = "子流水线添加的明细数量", required = true)
    val subPipelineCount: Long
)

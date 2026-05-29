package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务汇总信息")
interface PipelineBatchTaskSummary

@Schema(description = "流水线复制任务汇总信息")
data class PipelineCopyTaskSummary(
    @get:Schema(description = "未处理的资源数", required = true)
    val unprocessedCount: Int = 0,
    @get:Schema(description = "高风险资源数", required = true)
    val highRiskCount: Int = 0,
    @get:Schema(description = "自动完成的资源数量", required = true)
    val autoFinishCount: Int = 0
) : PipelineBatchTaskSummary

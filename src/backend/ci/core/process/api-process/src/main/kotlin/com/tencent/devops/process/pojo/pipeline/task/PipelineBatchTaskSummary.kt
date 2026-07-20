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
    @get:Schema(description = "资源需要补齐数量", required = true)
    val needCompletionCount: Int = 0,
    @get:Schema(description = "资源需要迁移数量", required = true)
    val needTransferCount: Int = 0,
    @get:Schema(description = "自动完成数量", required = true)
    val autoFinishCount: Int = 0,
) : PipelineBatchTaskSummary

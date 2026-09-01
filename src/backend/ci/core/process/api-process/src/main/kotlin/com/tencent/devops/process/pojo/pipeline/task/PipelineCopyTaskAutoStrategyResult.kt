package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制一键设置资源策略结果")
data class PipelineCopyTaskAutoStrategyResult(
    @get:Schema(description = "已自动设置策略的资源数量", required = true)
    val processedCount: Int,
    @get:Schema(description = "未自动设置的构建/部署节点数量", required = true)
    val nodeNotSetCount: Int,
    @get:Schema(description = "未自动设置的流水线冲突数量", required = true)
    val pipelineConflictCount: Int
)

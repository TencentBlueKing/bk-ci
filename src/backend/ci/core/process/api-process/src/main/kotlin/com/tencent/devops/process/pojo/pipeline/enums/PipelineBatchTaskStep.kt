package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务步骤")
enum class PipelineBatchTaskStep {
    @Schema(description = "配置任务信息")
    CONFIG,
    @Schema(description = "处理资源依赖")
    RESOURCE_DEPEND,
    @Schema(description = "执行任务")
    EXECUTE
}

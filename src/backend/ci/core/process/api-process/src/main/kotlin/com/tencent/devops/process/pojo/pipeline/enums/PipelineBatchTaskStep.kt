package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务步骤")
enum class PipelineBatchTaskStep {
    CONFIG,
    RESOURCE_DEPEND,
    EXECUTE
}

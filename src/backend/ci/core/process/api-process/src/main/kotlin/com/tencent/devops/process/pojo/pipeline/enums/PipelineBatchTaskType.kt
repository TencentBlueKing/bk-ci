package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务类型")
enum class PipelineBatchTaskType {
    PIPELINE_COPY
}

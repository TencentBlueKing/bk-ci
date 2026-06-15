package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线可见范围类型")
enum class PipelineVisibilityType {
    @Schema(title = "组织")
    ORG,

    @Schema(title = "用户")
    USER
}

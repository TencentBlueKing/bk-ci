package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制动作")
enum class PipelineCopyAction {
    @Schema(description = "自动完成")
    AUTO_FINISH,

    @Schema(description = "资源需要补齐")
    NEED_COMPLETION,

    @Schema(description = "资源需要迁移")
    NEED_TRANSFER
}

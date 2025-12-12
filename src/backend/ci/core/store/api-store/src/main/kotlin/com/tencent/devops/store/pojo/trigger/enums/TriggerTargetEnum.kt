package com.tencent.devops.store.pojo.trigger.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "触发目标")
enum class TriggerTargetEnum {
    @Schema(title = "创作流")
    CREATIVE,

    @Schema(title = "流水线")
    PIPELINE
}

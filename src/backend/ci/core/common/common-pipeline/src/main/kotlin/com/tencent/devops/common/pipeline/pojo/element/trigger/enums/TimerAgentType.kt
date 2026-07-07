package com.tencent.devops.common.pipeline.pojo.element.trigger.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "定时触发的启动节点类型（仅创作流通道使用）")
enum class TimerAgentType {
    @Schema(title = "指定创作节点")
    AGENT_LIST,

    @Schema(title = "枚举创作环境中的节点")
    ENV_ALL
}

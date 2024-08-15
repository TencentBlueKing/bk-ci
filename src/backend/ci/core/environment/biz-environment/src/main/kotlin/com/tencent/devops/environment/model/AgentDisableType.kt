package com.tencent.devops.environment.model

data class AgentDisableInfo(
    val type: AgentDisableType,
    val time: Long
)

enum class AgentDisableType {
    // Agent长时间不用被禁用
    AGENT_IDLE_DISABLED,

    // 项目禁用后禁用
    PROJECT_DISABLED;
}
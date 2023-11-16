package com.tencent.devops.common.api.pojo.agent

data class AgentErrorExitData(
    val errorEnum: AgentErrorExitErrorEnum,
    val message: String?
)

enum class AgentErrorExitErrorEnum {
    THIRD_AGENT_EXIT_NOT_WORKER,
    THIRD_AGENT_EXIT_LEFT_DEVICE,
    THIRD_AGENT_EXIT_PERMISSION_DENIED
}


package com.tencent.devops.common.environment.agent

enum class AgentUpgradeType {
    WORKER, GO_AGENT, JDK;

    companion object {
        fun find(type: String?): AgentUpgradeType? {
            return when (type) {
                WORKER.name -> WORKER
                GO_AGENT.name -> GO_AGENT
                JDK.name -> JDK
                else -> null
            }
        }
    }
}

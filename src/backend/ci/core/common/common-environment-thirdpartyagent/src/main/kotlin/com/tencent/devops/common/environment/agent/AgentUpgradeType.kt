package com.tencent.devops.common.environment.agent

enum class AgentUpgradeType {
    WORKER, GO_AGENT, JDK, DOCKER_INIT_FILE;

    companion object {
        fun find(type: String?): AgentUpgradeType? {
            return when (type) {
                WORKER.name -> WORKER
                GO_AGENT.name -> GO_AGENT
                JDK.name -> JDK
                DOCKER_INIT_FILE.name -> DOCKER_INIT_FILE
                else -> null
            }
        }
    }
}

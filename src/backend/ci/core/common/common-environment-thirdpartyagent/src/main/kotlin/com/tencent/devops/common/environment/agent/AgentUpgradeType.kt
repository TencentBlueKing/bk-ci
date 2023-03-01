package com.tencent.devops.common.environment.agent

enum class AgentUpgradeType {
    WORKER, GO_AGENT, JDK, DOCKER_INIT_FILE, TELEGRAF_CONF;

    companion object {
        fun find(type: String?): AgentUpgradeType? {
            return when (type) {
                WORKER.name -> WORKER
                GO_AGENT.name -> GO_AGENT
                JDK.name -> JDK
                DOCKER_INIT_FILE.name -> DOCKER_INIT_FILE
                TELEGRAF_CONF.name -> TELEGRAF_CONF
                else -> null
            }
        }
    }
}

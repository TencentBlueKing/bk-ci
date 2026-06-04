package com.tencent.devops.environment.pojo.imate

import com.tencent.devops.common.api.pojo.OS

data class ImateListItem(
    val name: String,
    val deviceId: String,
    val ip: String?,
    val os: OS,
    val engine: ImateItemEngine,
    val status: String?
)

enum class ImateItemEngine {
    Unknown,
    OpenClaw,
    Hermes,
    ;
}

enum class ImateOriginEngine(val value: String) {
    DEVCLOUD("devcloud"), // 个人 openclaw
    TEAM_DEVCLOUD("team_devcloud"), // 共享 openclaw
    HERMES_DEVCLOUD("hermes_devcloud"), // 个人 hermes
    TEAM_HERMES_DEVCLOUD("team_hermes_devcloud"), // 共享 hermes
    ;

    companion object {
        fun toEngine(value: String?) = when (value) {
            DEVCLOUD.value -> ImateItemEngine.OpenClaw
            TEAM_DEVCLOUD.value -> ImateItemEngine.OpenClaw
            HERMES_DEVCLOUD.value -> ImateItemEngine.Hermes
            TEAM_HERMES_DEVCLOUD.value -> ImateItemEngine.Hermes
            else -> ImateItemEngine.Unknown
        }
    }
}

data class ImportImageNodeDataItem(
    val name: String,
    val deviceId: String
)
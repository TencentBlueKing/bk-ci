package com.tencent.devops.environment.model

import com.tencent.devops.common.api.pojo.agent.DockerInitFileInfo
import com.tencent.devops.common.api.pojo.agent.AgentErrorExitData

/**
 * Agent 系统属性
 * @see com.tencent.devops.environment.model arch 系统架构
 * @param jdkVersion jdk版本
 * @param dockerInitFileInfo dockerInit文件信息
 * @param exitError agent错误退出信息
 * @param osVersion 系统版本信息
 * @param source 来源信息
 */
data class AgentProps(
    val arch: String,
    val jdkVersion: List<String>,
    val userProps: Map<String, Any>?,
    val dockerInitFileInfo: DockerInitFileInfo?,
    val exitError: AgentErrorExitData?,
    val osVersion: String?,
    val source: AgentPropsSource? = null
) {
    companion object {
        fun emptyBySource(source: AgentPropsSource) = AgentProps(
            arch = "",
            jdkVersion = emptyList(),
            userProps = emptyMap(),
            dockerInitFileInfo = null,
            exitError = null,
            osVersion = null,
            source = source
        )
    }
}

enum class AgentPropsSource {
    REMOTEDEV, // 云桌面
    DEVCOUD, // 团队imate龙虾
    ;
}

package com.tencent.devops.environment.model

import com.tencent.devops.common.api.pojo.agent.DockerInitFileInfo
import com.tencent.devops.common.api.pojo.agent.AgentErrorExitData
import com.tencent.devops.common.api.util.JsonUtil

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
    val source: AgentPropsSource? = null,
    val sdk: Boolean? = false
) {
    companion object {
        fun parseAgentProps(props: String?): AgentProps? {
            return if (props.isNullOrBlank()) {
                null
            } else {
                try {
                    JsonUtil.to(props, AgentProps::class.java)
                } catch (e: Exception) {
                    // 兼容老数据格式不对的情况
                    null
                }
            }
        }
        fun emptyBySource(source: AgentPropsSource) = AgentProps(
            arch = "",
            jdkVersion = emptyList(),
            userProps = emptyMap(),
            dockerInitFileInfo = null,
            exitError = null,
            osVersion = null,
            source = source
        )
        fun emptyBySdk(sdk: Boolean) = AgentProps(
            arch = "",
            jdkVersion = emptyList(),
            userProps = emptyMap(),
            dockerInitFileInfo = null,
            exitError = null,
            osVersion = null,
            source = null,
            sdk = sdk
        )
    }
}

enum class AgentPropsSource {
    REMOTEDEV, // 云桌面
    DEVCLOUD, // 团队imate龙虾
    ;
}

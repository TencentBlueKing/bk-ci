package com.tencent.devops.environment.model

import com.tencent.devops.common.api.pojo.agent.DockerInitFileInfo

/**
 * Agent 系统属性
 * @see com.tencent.devops.environment.model arch 系统架构
 * @param jdkVersion jdk版本
 * @param dockerInitFileInfo dockerInit文件信息
 */
data class AgentProps(
    val arch: String,
    val jdkVersion: List<String>,
    val userProps: Map<String, Any>?,
    val dockerInitFileInfo: DockerInitFileInfo?
)

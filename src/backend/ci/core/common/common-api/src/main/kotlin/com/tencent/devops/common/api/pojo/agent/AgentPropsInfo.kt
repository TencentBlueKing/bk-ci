package com.tencent.devops.common.api.pojo.agent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Agent属性信息")
data class AgentPropsInfo(
    @Schema(description = "agent运行系统的架构信息")
    val arch: String,
    @Schema(description = "jdk版本信息")
    val jdkVersion: List<String>?,
    @Schema(description = "docker init 文件升级信息")
    val dockerInitFileInfo: DockerInitFileInfo?
)

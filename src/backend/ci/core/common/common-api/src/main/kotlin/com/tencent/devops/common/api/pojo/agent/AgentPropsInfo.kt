package com.tencent.devops.common.api.pojo.agent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Agent属性信息")
data class AgentPropsInfo(
    @Schema(name = "agent运行系统的架构信息")
    val arch: String,
    @Schema(name = "jdk版本信息")
    val jdkVersion: List<String>?,
    @Schema(name = "docker init 文件升级信息")
    val dockerInitFileInfo: DockerInitFileInfo?
)

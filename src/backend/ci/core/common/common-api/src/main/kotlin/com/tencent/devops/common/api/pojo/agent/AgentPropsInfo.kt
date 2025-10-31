package com.tencent.devops.common.api.pojo.agent

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Agent属性信息")
data class AgentPropsInfo(
    @get:Schema(title = "agent运行系统的架构信息")
    val arch: String,
    @get:Schema(title = "jdk版本信息")
    val jdkVersion: List<String>?,
    @JsonProperty("dockerInitFileMd5")
    @get:Schema(title = "docker init 文件升级信息")
    val dockerInitFileInfo: DockerInitFileInfo?,
    val osVersion: String?
)

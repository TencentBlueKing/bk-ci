package com.tencent.devops.common.api.pojo.agent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Agent属性信息")
data class AgentPropsInfo(
    @ApiModelProperty("agent运行系统的架构信息")
    val arch: String,
    @ApiModelProperty("jdk版本信息")
    val jdkVersion: List<String>?,
    @ApiModelProperty("docker init 文件升级信息")
    val dockerInitFileInfo: DockerInitFileInfo?
)

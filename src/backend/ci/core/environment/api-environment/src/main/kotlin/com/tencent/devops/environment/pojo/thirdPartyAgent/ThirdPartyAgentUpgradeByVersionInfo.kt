package com.tencent.devops.environment.pojo.thirdPartyAgent

import com.tencent.devops.common.api.pojo.agent.DockerInitFileInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("第三方构建机检查版本升级上报的信息")
data class ThirdPartyAgentUpgradeByVersionInfo(
    @ApiModelProperty("work版本")
    val workerVersion: String?,
    @ApiModelProperty("go agent 版本")
    val goAgentVersion: String?,
    @ApiModelProperty("jdk版本")
    val jdkVersion: List<String>?,
    @ApiModelProperty("docker init 文件升级信息")
    val dockerInitFileInfo: DockerInitFileInfo?
)

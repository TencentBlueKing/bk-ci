package com.tencent.devops.environment.pojo.thirdPartyAgent

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

@ApiModel("docker init 文件升级信息")
data class DockerInitFileInfo(
    @ApiModelProperty("文件md5值")
    val fileMd5: String,
    @ApiModelProperty("目前只支持linux机器，所以其他系统不需要检查")
    val needUpgrade: Boolean
)

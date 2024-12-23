package com.tencent.devops.environment.pojo.thirdpartyagent

import com.tencent.devops.common.api.pojo.agent.DockerInitFileInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "第三方构建机检查版本升级上报的信息")
data class ThirdPartyAgentUpgradeByVersionInfo(
    @get:Schema(title = "work版本")
    val workerVersion: String?,
    @get:Schema(title = "go agent 版本")
    val goAgentVersion: String?,
    @get:Schema(title = "jdk版本")
    val jdkVersion: List<String>?,
    @get:Schema(title = "docker init 文件升级信息")
    val dockerInitFileInfo: DockerInitFileInfo?
)

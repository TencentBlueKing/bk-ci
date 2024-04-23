package com.tencent.devops.dispatch.pojo.thirdpartyagent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.environment.pojo.thirdpartyagent.AskHeartbeatResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentPipeline
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentUpgradeByVersionInfo

@JsonIgnoreProperties(ignoreUnknown = true)
data class ThirdPartyAskInfo(
    val askEnable: AskEnable,
    val heartbeat: NewHeartbeatInfo,
    val upgrade: ThirdPartyAgentUpgradeByVersionInfo?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AskEnable(
    val build: String,
    val upgrade: Boolean,
    val dockerDebug: Boolean,
    val pipeline: Boolean
)

data class ThirdPartyAskResp(
    val heartbeat: AskHeartbeatResponse?,
    val build: ThirdPartyBuildInfo?,
    val upgrade: UpgradeItem?,
    val pipeline: ThirdPartyAgentPipeline?,
    val debug: ThirdPartyDockerDebugInfo?
)

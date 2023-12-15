package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentUpgradeByVersionInfo

data class ThirdPartyAskInfo(
    val build: ThirdPartyAskBuild,
    val upgrade: ThirdPartyAgentUpgradeByVersionInfo
)

data class ThirdPartyAskBuild(
    val buildType: String?
)

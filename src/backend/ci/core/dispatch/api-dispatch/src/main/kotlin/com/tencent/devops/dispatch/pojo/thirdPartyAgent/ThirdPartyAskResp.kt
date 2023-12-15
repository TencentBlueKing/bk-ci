package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import com.tencent.devops.common.api.pojo.agent.UpgradeItem

data class ThirdPartyAskResp(
    val build: ThirdPartyBuildInfo?,
    val upgrade: UpgradeItem,
    val dockerBuild: ThirdPartyDockerDebugInfo?
)

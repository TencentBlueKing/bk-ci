package com.tencent.devops.common.environment.agent.pojo.devcloud

data class Pool(
    val container: String?,
    val credential: Credential?,
    val performanceConfigId: String? = "0",
    val third: Boolean? = true
)

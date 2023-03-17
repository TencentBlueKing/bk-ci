package com.tencent.devops.dispatch.devcloud.pojo

data class Pool(
    val container: String?,
    val credential: Credential?,
    val performanceConfigId: String? = "0",
    val third: Boolean? = true
)

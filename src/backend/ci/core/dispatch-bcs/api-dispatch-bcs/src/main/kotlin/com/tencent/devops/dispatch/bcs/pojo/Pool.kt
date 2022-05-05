package com.tencent.devops.dispatch.bcs.pojo

data class Pool(
    val container: String?,
    val credential: Credential?,
    val performanceConfigId: String? = "0",
    val third: Boolean? = true
)

data class Credential(
    val user: String,
    val password: String
)

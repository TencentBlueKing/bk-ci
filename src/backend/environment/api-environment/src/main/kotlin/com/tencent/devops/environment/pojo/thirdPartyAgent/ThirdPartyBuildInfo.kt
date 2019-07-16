package com.tencent.devops.environment.pojo.thirdPartyAgent

data class ThirdPartyBuildInfo(
    val projectId: String,
    val buildId: String,
    val vmSeqId: String,
    val workspace: String
)
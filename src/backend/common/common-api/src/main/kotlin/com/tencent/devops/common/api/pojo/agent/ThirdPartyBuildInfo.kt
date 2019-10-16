package com.tencent.devops.common.api.pojo.agent

data class ThirdPartyBuildInfo(
    val projectId: String,
    val buildId: String,
    val vmSeqId: String,
    val workspace: String
)
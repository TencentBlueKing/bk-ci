package com.tencent.devops.dispatch.pojo.thirdPartyAgent

data class ThirdPartyBuildWithStatus(
    val projectId: String,
    val buildId: String,
    val vmSeqId: String,
    val workspace: String,
    val pipelineId: String?,
    val success: Boolean,
    val message: String?
)
package com.tencent.devops.buildless.pojo

data class BuildLessStartInfo(
    val projectId: String,
    val agentId: String,
    val pipelineId: String,
    val buildId: String,
    val vmSeqId: Int,
    val secretKey: String,
    val poolNo: Int,
    val buildType: String,
)

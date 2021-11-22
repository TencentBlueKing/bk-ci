package com.tencent.devops.buildless.pojo

data class BuildLessEndInfo(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val vmSeqId: Int,
    val containerId: String,
    val poolNo: Int
)

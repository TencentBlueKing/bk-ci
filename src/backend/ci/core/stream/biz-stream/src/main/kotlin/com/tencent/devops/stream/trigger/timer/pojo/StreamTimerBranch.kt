package com.tencent.devops.stream.trigger.timer.pojo

data class StreamTimerBranch(
    val projectId: String,
    val pipelineId: String,
    val gitProjectId: Long,
    val branch: String,
    val revision: String
)

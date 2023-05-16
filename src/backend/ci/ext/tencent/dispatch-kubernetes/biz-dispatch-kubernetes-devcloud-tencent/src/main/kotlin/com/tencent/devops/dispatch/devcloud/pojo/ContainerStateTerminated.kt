package com.tencent.devops.dispatch.devcloud.pojo

data class ContainerStateTerminated(
    val exitCode: Int,
    val signal: Int,
    val reason: String,
    val message: String,
    val startedAt: String,
    val finishedAt: String,
    val containerID: String
)

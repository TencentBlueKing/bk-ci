package com.tencent.devops.dispatch.devcloud.pojo

import com.tencent.devops.dispatch.kubernetes.pojo.devcloud.Proto3Timestamp

data class ContainerStateTerminated(
    val exitCode: Int,
    val signal: Int,
    val reason: String,
    val message: String,
    val startedAt: Proto3Timestamp,
    val finishedAt: Proto3Timestamp,
    val containerID: String,
)

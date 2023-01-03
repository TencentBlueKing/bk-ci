package com.tencent.devops.dispatch.devcloud.pojo

import com.tencent.devops.dispatch.kubernetes.pojo.devcloud.Proto3Timestamp

data class ContainerStateRunning(
    val startedAt: Proto3Timestamp
)

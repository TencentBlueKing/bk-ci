package com.tencent.devops.dispatch.devcloud.pojo

import com.tencent.devops.dispatch.kubernetes.pojo.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.devcloud.Proto3Timestamp

data class EnvironmentStatus(
    val status: EnvStatusEnum,
    val phase: String,
    val message: String,
    val reason: String,
    val hostIP: String,
    val EnvironmentIP: String,
    val clusterId: String,
    val namespace: String,
    val startTime: Proto3Timestamp,
    val initContainerStatuses: ContainerStatus,
    val containerStatuses: ContainerStatus
)

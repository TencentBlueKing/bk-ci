package com.tencent.devops.dispatch.devcloud.pojo

import com.tencent.devops.dispatch.kubernetes.pojo.EnvStatusEnum

data class EnvironmentStatus(
    val status: EnvStatusEnum,
    val phase: String,
    val message: String,
    val reason: String,
    val hostIP: String,
    val EnvironmentIP: String,
    val clusterId: String,
    val namespace: String,
    val startTime: String,
    val initContainerStatuses: ContainerStatus,
    val containerStatuses: ContainerStatus
)

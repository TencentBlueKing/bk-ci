package com.tencent.devops.dispatch.devcloud.pojo

import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum

data class EnvironmentStatus(
    val status: EnvStatusEnum,
    val phase: String,
    val message: String,
    val reason: String,
    val hostIP: String,
    val environmentIP: String,
    val clusterId: String,
    val namespace: String,
    val startTime: String ? = null,
    val containerStatuses: List<ContainerStatus>,
    val initContainerStatuses: List<ContainerStatus> ? = null
)

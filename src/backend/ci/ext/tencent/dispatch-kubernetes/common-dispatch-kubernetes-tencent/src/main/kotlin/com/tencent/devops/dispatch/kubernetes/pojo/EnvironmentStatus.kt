package com.tencent.devops.dispatch.kubernetes.pojo

import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum

data class EnvironmentStatus(
    val status: EnvStatusEnum,
    val phase: String? = null,
    val message: String ? = null,
    val reason: String ? = null,
    val hostIP: String,
    val podIP: String ? = null,
    val environmentIP: String ? = null,
    val clusterId: String,
    val namespace: String,
    val name: String ? = null,
    val startTime: String ? = null,
    val containerStatuses: List<ContainerStatus>,
    val initContainerStatuses: List<ContainerStatus> ? = null
)

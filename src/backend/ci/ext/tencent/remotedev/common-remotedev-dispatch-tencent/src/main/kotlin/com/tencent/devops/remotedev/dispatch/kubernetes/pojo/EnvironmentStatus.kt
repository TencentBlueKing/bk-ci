package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentStatus(
    val status: EnvStatusEnum,
    val phase: String? = null,
    val message: String? = null,
    val reason: String? = null,
    val hostIP: String,
    val environmentIP: String,
    val clusterId: String?,
    val namespace: String?,
    val name: String? = null,
    val startTime: String? = null,
    val containerStatuses: List<ContainerStatus>? = null,
    val initContainerStatuses: List<ContainerStatus>? = null
)

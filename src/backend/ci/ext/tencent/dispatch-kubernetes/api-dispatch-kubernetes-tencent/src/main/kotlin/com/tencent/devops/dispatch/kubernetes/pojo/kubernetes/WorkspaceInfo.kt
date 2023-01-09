package com.tencent.devops.dispatch.kubernetes.pojo.kubernetes

data class WorkspaceInfo(
    val status: EnvStatusEnum,
    val hostIP: String,
    val EnvironmentIP: String,
    val clusterId: String,
    val namespace: String,
    val environmentHost: String
)

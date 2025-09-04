package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class ContainerPort(
    val name: String,
    val hostport: Int,
    val containerPort: Int,
    val protocol: String,
    val hostIP: String
)

package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class ContainerStateWaiting(
    val reason: String,
    val message: String
)

package com.tencent.devops.dispatch.kubernetes.pojo

data class ContainerStateWaiting(
    val reason: String,
    val message: String
)

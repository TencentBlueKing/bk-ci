package com.tencent.devops.dispatch.kubernetes.pojo

data class Lifecycle(
    val postStart: LifecycleHandler,
    val preStop: LifecycleHandler
)

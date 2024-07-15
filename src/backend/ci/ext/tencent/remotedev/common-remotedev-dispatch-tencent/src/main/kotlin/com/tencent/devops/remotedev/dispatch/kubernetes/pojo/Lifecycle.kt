package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class Lifecycle(
    val postStart: LifecycleHandler,
    val preStop: LifecycleHandler
)

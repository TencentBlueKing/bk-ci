package com.tencent.devops.dispatch.bcs.pojo

data class Lifecycle(
    val postStart: LifecycleHandler,
    val preStop: LifecycleHandler
)

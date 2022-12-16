package com.tencent.devops.dispatch.devcloud.pojo

data class Lifecycle(
    val postStart: LifecycleHandler,
    val preStop: LifecycleHandler
)

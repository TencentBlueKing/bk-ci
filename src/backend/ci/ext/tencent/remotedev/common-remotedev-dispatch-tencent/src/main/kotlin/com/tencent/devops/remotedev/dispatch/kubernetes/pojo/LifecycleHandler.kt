package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class LifecycleHandler(
    val exec: ExecAction,
    val httpGet: HTTPGetAction
)

package com.tencent.devops.dispatch.kubernetes.pojo

data class LifecycleHandler(
    val exec: ExecAction,
    val httpGet: HTTPGetAction
)

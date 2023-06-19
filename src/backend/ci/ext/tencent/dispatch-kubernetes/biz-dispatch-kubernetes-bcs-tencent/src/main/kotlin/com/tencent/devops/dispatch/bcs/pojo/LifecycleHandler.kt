package com.tencent.devops.dispatch.bcs.pojo

data class LifecycleHandler(
    val exec: ExecAction,
    val httpGet: HTTPGetAction
)

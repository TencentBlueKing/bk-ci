package com.tencent.devops.dispatch.devcloud.pojo

data class LifecycleHandler(
    val exec: ExecAction,
    val httpGet: HTTPGetAction
)

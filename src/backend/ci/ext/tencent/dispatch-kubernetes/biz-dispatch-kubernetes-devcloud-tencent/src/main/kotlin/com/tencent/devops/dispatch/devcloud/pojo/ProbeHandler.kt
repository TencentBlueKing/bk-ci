package com.tencent.devops.dispatch.devcloud.pojo

data class ProbeHandler(
    val exec: ExecAction,
    val httpGet: HTTPGetAction
)

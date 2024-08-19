package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class ProbeHandler(
    val exec: ExecAction? = null,
    val httpGet: HTTPGetAction? = null
)

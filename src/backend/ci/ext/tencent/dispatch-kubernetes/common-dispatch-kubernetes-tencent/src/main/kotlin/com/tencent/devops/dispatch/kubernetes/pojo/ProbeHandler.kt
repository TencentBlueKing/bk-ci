package com.tencent.devops.dispatch.kubernetes.pojo

data class ProbeHandler(
    val exec: ExecAction? = null,
    val httpGet: HTTPGetAction? = null
)

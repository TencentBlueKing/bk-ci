package com.tencent.devops.dispatch.bcs.pojo

data class ProbeHandler(
    val exec: ExecAction? = null,
    val httpGet: HTTPGetAction? = null
)

package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class EnvironmentListReq(
    val username: String,
    val offset: Int,
    val length: Int
)

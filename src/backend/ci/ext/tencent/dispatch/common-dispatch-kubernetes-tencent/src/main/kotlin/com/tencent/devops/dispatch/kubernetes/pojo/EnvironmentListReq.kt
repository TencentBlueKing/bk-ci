package com.tencent.devops.dispatch.kubernetes.pojo

data class EnvironmentListReq(
    val username: String,
    val offset: Int,
    val length: Int
)

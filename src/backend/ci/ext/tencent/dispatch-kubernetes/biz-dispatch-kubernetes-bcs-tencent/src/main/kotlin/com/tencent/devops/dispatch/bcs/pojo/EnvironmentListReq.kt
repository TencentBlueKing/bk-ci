package com.tencent.devops.dispatch.bcs.pojo

data class EnvironmentListReq(
    val username: String,
    val offset: Int,
    val length: Int
)

package com.tencent.devops.dispatch.bcs.pojo

data class EnvironmentStatusRsp(
    val code: Int,
    val message: String,
    val data: EnvironmentStatus
)

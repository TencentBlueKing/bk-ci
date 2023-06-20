package com.tencent.devops.dispatch.kubernetes.pojo

data class EnvironmentStatusRsp(
    val code: Int,
    val message: String,
    val data: EnvironmentStatus
)

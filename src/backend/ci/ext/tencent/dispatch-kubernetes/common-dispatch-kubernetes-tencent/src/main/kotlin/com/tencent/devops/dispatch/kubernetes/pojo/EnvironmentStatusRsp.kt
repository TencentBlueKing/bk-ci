package com.tencent.devops.dispatch.kubernetes.pojo

data class EnvironmentStatusRsp(
    val result: Boolean? = false,
    val code: Int,
    val message: String,
    val data: EnvironmentStatus
)

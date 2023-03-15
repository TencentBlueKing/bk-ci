package com.tencent.devops.dispatch.devcloud.pojo

data class EnvironmentStatusRsp(
    val code: Int,
    val message: String,
    val data: EnvironmentStatus
)

package com.tencent.devops.dispatch.kubernetes.pojo

data class EnvironmentDetailRsp(
    val code: Int,
    val message: String,
    val data: Environment
)

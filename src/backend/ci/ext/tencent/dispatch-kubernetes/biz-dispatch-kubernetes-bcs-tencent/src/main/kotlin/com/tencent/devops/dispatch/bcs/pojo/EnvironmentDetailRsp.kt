package com.tencent.devops.dispatch.bcs.pojo

data class EnvironmentDetailRsp(
    val code: Int,
    val message: String,
    val data: Environment
)

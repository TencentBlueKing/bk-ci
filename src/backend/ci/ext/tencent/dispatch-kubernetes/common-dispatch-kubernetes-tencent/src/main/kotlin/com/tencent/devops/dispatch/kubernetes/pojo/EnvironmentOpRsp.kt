package com.tencent.devops.dispatch.kubernetes.pojo

data class EnvironmentOpRsp(
    val code: Int,
    val message: String,
    val data: EnvironmentOpRspData
)

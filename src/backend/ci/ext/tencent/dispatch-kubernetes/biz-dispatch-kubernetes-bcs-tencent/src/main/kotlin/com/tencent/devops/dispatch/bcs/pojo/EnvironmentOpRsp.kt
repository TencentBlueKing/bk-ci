package com.tencent.devops.dispatch.bcs.pojo

data class EnvironmentOpRsp(
    val code: Int,
    val message: String,
    val data: EnvironmentOpRspData
)

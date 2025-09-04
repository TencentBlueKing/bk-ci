package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class EnvironmentOpRsp(
    val code: Int,
    val message: String,
    val data: EnvironmentOpRspData,
    val result: Boolean?
)

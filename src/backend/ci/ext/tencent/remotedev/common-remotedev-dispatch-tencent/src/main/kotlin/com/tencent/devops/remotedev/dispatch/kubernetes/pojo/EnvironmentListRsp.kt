package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class EnvironmentListRsp(
    val code: Int,
    val message: String,
    val data: List<Environment>,
    val total: Int
)

package com.tencent.devops.dispatch.devcloud.pojo

data class EnvironmentListRsp(
    val code: Int,
    val message: String,
    val data: List<Environment>,
    val total: Int
)

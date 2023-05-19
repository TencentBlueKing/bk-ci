package com.tencent.devops.dispatch.startCloud.pojo

data class EnvironmentCreateRsp(
    val code: Int,
    val data: EnvironmentCreateRspData,
    val message: String
) {
    data class EnvironmentCreateRspData(
        val cgsIp: String
    )
}

package com.tencent.devops.dispatch.startCloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentCreateRsp(
    val code: Int,
    val data: EnvironmentCreateRspData?,
    val message: String
) {
    data class EnvironmentCreateRspData(
        val cgsIp: String
    )
}

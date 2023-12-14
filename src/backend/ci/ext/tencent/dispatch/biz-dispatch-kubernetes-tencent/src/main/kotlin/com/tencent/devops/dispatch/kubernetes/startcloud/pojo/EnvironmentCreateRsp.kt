package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentCreateRsp(
    val code: Int,
    val data: EnvironmentCreateRspData?,
    val message: String
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EnvironmentCreateRspData(
        val taskUid: String,
        val environmentUid: String
    )
}

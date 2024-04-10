package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentOperateRsp(
    val code: Int,
    val data: EnvironmentOperateRspData?,
    val message: String
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EnvironmentOperateRspData(
        val taskUid: String
    )
}

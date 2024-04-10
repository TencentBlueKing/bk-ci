package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentLockedVmRsp(
    val code: Int,
    val message: String,
    val result: Boolean,
    val data: EnvironmentLockedVmRspData
) {
    data class EnvironmentLockedVmRspData(
        val vmList: List<String>
    )
}

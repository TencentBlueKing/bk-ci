package com.tencent.devops.dispatch.bcs.pojo

data class DispatchBuildStatusResp(
    val status: String,
    val errorMsg: String? = null
)

enum class DispatchBuildStatusEnum {
    running,
    succeeded,
    failed
}

package com.tencent.devops.dispatch.bcs.pojo

data class DispatchJobLogResp(
    val log: List<String>?,
    val errorMsg: String? = null
)

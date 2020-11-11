package com.tencent.devops.common.api.pojo

data class ToolRunResult(
    val toolName : String,
    val errorMsg : String,
    val errCode: Int,
    val startTime : Long,
    val endTime : Long,
    val status : ToolRunResultStatus
){
    enum class ToolRunResultStatus {
        SUCCESS,
        FAIL,
        SKIP,
        TIMEOUT
    }
}
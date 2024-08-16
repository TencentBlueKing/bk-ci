package com.tencent.devops.dispatch.devcloud.pojo.devcloud

data class JobResponse(
    val actionCode: Int,
    val actionMessage: String,
    val data: JobResponseData
) {
    data class JobResponseData(
        val name: String,
        val taskId: Int
    )
}

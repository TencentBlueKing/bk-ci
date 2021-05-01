package com.tencent.bk.devops.plugin.docker.pojo.job.request

data class JobResponse (
    val actionCode: Int,
    val actionMessage: String,
    val data: JobResponseData
) {
    data class JobResponseData (
        val name: String,
        val taskId: Int
    )
}

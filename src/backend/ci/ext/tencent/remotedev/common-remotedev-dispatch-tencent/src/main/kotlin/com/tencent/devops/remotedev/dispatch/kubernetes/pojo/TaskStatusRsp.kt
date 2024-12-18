package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


data class TaskStatusRsp(
    val result: Boolean? = false,
    val code: Int,
    val message: String,
    val data: TaskStatus?
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TaskStatus(
        val status: String,
        val currentStep: String? = null
    )
}

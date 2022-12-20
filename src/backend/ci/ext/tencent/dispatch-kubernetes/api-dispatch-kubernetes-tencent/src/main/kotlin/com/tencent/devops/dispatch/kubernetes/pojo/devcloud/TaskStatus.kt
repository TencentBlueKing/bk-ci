package com.tencent.devops.dispatch.kubernetes.pojo.devcloud

data class TaskStatus(
    val uid: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val status: TaskStatusEnum? = null,
    val statuscode: Int? = null,
    val logs: String? = null
)

enum class TaskStatusEnum {
    Pending,
    Running,
    Success,
    Fail,
    Abort
}

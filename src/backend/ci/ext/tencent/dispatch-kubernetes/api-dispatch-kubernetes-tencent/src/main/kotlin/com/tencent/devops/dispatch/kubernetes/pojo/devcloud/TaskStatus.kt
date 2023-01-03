package com.tencent.devops.dispatch.kubernetes.pojo.devcloud

data class TaskStatus(
    val uid: String,
    val createdAt: Proto3Timestamp? = null,
    val updatedAt: Proto3Timestamp? = null,
    val status: TaskStatusEnum? = null,
    val statuscode: Int? = null,
    val logs: List<String> = emptyList()
)

enum class TaskStatusEnum {
    Pending,
    Running,
    Success,
    Fail,
    Abort
}

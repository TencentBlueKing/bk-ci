package com.tencent.devops.dispatch.devcloud.pojo

data class TaskStatus(
    val uid: String,
    val createdAt: String,
    val updatedAt: String,
    val status: TaskStatusEnum,
    val statuscode: Int,
    val logs: String
)

enum class TaskStatusEnum {
    Pending,
    Running,
    Success,
    Fail,
    Abort
}

package com.tencent.devops.dispatch.devcloud.pojo

data class TaskStatus(
    val uid: String,
    val createdAt: String,
    val updatedAt: String,
    val status: String,
    val statuscode: Int,
    val logs: String
)

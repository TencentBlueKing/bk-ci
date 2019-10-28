package com.tencent.devops.image.pojo

data class PushImageTask(
    var taskId: String,
    var projectId: String,
    var operator: String,
    var createdTime: Long,
    var updatedTime: Long,
    var taskStatus: String,
    var taskMessage: String
)

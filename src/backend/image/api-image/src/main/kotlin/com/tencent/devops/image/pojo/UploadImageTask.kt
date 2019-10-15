package com.tencent.devops.image.pojo

data class UploadImageTask(
    var taskId: String,
    var projectId: String,
    var operator: String,
    var createdTime: Long,
    var updatedTime: Long,
    val taskStatus: String,
    val taskMessage: String,
    val imageData: List<DockerImage>
)

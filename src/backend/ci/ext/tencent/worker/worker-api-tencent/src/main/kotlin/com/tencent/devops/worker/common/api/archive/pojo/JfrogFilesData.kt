package com.tencent.devops.worker.common.task.archive.pojo

data class JfrogFilesData(
    val uri: String,
    val created: String,
    val files: List<JfrogFile>
)

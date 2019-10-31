package com.tencent.devops.artifactory.service.pojo

data class FileShareInfo(
    val fileName: String,
    val md5: String,
    val projectName: String,
    val downloadUrl: String
)
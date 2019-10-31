package com.tencent.devops.artifactory.service.pojo

data class JFrogFileInfo(
    val uri: String,
    val size: Long,
    val lastModified: String,
    val folder: Boolean
)
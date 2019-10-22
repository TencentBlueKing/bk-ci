package com.tencent.devops.common.archive.api.pojo

data class JFrogFileInfo(
    val uri: String,
    val size: Long,
    val lastModified: String,
    val folder: Boolean
)
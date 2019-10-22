package com.tencent.devops.common.archive.api.pojo

data class JFrogFileInfoList(
    val uri: String,
    val created: String,
    val files: List<JFrogFileInfo>
)
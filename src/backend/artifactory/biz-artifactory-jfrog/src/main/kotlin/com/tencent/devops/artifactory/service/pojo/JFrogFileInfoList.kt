package com.tencent.devops.artifactory.service.pojo

data class JFrogFileInfoList(
    val uri: String,
    val created: String,
    val files: List<JFrogFileInfo>
)
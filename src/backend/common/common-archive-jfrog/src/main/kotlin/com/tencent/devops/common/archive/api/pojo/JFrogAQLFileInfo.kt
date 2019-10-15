package com.tencent.devops.common.archive.api.pojo

data class JFrogAQLFileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val created: String,
    val modified: String,
    val properties: List<JFrogProperty>?
)
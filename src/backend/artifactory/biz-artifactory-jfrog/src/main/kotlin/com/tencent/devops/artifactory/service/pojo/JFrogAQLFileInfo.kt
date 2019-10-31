package com.tencent.devops.artifactory.service.pojo

data class JFrogAQLFileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val created: String,
    val modified: String,
    val properties: List<JFrogProperty>?
)
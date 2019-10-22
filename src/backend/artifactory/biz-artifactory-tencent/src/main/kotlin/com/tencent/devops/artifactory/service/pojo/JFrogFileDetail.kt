package com.tencent.devops.artifactory.service.pojo

data class JFrogFileDetail(
    val path: String,
    val size: Long,
    val created: String,
    val lastModified: String,
    val checksums: CheckSums?
)
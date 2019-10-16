package com.tencent.devops.common.archive.api.pojo

data class JFrogFileDetail(
    val path: String,
    val size: Long,
    val created: String,
    val lastModified: String,
    val checksums: CheckSums?
)
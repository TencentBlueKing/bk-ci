package com.tencent.devops.artifactory.service.pojo

import com.tencent.devops.common.archive.api.pojo.CheckSums

data class JFrogFileDetail(
    val path: String,
    val size: Long,
    val created: String,
    val lastModified: String,
    val checksums: CheckSums?
)
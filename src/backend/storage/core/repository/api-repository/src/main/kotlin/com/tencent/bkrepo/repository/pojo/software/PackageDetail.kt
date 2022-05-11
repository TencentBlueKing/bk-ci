package com.tencent.bkrepo.repository.pojo.software

import com.tencent.bkrepo.repository.pojo.packages.PackageType
import java.time.LocalDateTime

data class PackageDetail(
    val projectId: String,
    val repoName: String,
    val packageName: String,
    val key: String,
    val type: PackageType,
    val name: String,
    val downloads: Long,
    val size: Long,
    val lastModifiedDate: LocalDateTime,
    val packageId: String
)

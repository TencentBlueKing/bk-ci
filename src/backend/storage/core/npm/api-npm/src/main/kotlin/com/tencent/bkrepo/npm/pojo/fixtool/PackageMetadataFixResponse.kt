package com.tencent.bkrepo.npm.pojo.fixtool

data class PackageMetadataFixResponse(
    val projectId: String,
    val repoName: String,
    val successCount: Long = 0L,
    val failedCount: Long = 0L
)

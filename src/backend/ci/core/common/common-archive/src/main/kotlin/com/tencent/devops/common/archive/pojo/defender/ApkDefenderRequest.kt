package com.tencent.devops.common.archive.pojo.defender

data class ApkDefenderRequest(
    val projectId: String,
    val repoName: String,
    val fullPath: String,
    val scanner: String,
    val users: Collection<String>,
    val batchSize: Int
)

package com.tencent.bkrepo.helm.pojo.fixtool

data class DateTimeRepairResponse(
    val successRepoName: List<RepairResponse>,
    val failRepoName: List<RepairResponse>
)

data class RepairResponse(
    val projectId: String,
    val repoName: String
)

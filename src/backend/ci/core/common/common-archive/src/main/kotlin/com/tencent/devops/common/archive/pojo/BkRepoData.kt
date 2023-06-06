package com.tencent.devops.common.archive.pojo

data class BkRepoData(
    val pageNumber: Int,
    val pageSize: Int,
    val totalRecords: Long,
    val totalPages: Long,
    val records: List<BkRepoInfo>
)

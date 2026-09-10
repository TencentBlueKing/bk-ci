package com.tencent.devops.process.pojo

import java.time.LocalDateTime

data class BatchFetchBuildRecordData(
    val buildIds: List<String>,
    val executeCount: Int?
)

data class BatchFetchContainerRecordData(
    val buildId: String,
    val containerIds: List<String>,
    val executeCount: Int
)


data class BatchFetchRecordResp(
    val buildId: String,
    val startUser: String,
    val status: String?,
    val executeCount: Int,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?
)

data class BatchFetchContainerRecordResp(
    val containerId: String,
    val status: String?,
    val executeCount: Int,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?
)
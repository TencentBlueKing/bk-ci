package com.tencent.devops.dispatch.pojo.thirdpartyagent

import java.time.LocalDateTime

data class TPAPipelineJobBuild(
    val buildNo: Int,
    val buildStatus: String,
    val timeInterval: Long,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val userId: String
)

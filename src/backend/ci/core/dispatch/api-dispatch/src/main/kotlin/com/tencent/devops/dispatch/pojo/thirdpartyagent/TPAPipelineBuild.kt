package com.tencent.devops.dispatch.pojo.thirdpartyagent

import java.time.LocalDateTime

data class TPAPipelineBuild(
    val pipelineId: String,
    val pipelineName: String,
    val jobId: String?,
    val jobName: String?,
    val buildCount: Int,
    val lastBuildTime: LocalDateTime,
    val firstBuildTime: LocalDateTime,
    val avgTimeInterval: Long? // 平均耗时（毫秒）
)
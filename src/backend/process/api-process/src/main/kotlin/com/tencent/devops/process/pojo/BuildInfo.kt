package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode

data class BuildInfo(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val version: Int,
    val buildNum: Int,
    val trigger: String,
    val status: BuildStatus,
    val startUser: String,
    val startTime: Long?,
    val endTime: Long?,
    val taskCount: Int,
    val firstTaskId: String,
    val parentBuildId: String?,
    val parentTaskId: String?,
    val channelCode: ChannelCode,
    var errorType: ErrorType?,
    var errorCode: Int?,
    var errorMsg: String?
)

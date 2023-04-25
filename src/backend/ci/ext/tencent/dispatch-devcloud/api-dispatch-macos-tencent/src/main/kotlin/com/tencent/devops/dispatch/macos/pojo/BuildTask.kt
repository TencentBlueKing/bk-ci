package com.tencent.devops.dispatch.macos.pojo

import io.swagger.annotations.ApiModel

@ApiModel("构建任务")
data class BuildTask(
    var buildId: String,
    var vmSeqId: String,
    var vmIp: String,
    var buildHistoryId: Int
)

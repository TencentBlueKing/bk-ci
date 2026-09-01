package com.tencent.devops.remotedev.pojo.cvd

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD任务状态查询请求")
data class CvdTaskStatusRequest(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String
)

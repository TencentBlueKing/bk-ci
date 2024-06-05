package com.tencent.devops.common.pipeline.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "心跳请求报文体")
data class JobHeartbeatRequest(
    @get:Schema(title = "task执行速率", required = false)
    val task2ProgressRate: Map<String, Double>?
)

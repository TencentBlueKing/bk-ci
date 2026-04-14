package com.tencent.devops.remotedev.pojo.cvd

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD退回云桌面请求")
data class CvdDeleteTaskRequest(
    @get:Schema(description = "用户ID", required = true)
    val username: String,
    @get:Schema(description = "蓝盾项目ID", required = true)
    val bkProjectId: String,
    @get:Schema(description = "资源池ID", required = true)
    val poolId: String,
    @get:Schema(description = "云桌面实例ID", required = true)
    val instanceId: String
)

package com.tencent.devops.remotedev.pojo.cvd

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD领用云桌面请求")
data class CvdCreateTaskRequest(
    @get:Schema(description = "用户ID", required = true)
    val username: String,
    @get:Schema(description = "蓝盾项目ID", required = true)
    val bkProjectId: String,
    @get:Schema(description = "资源池ID", required = true)
    val poolId: String,
    @get:Schema(description = "磁盘ID，一期默认为空", required = false)
    val diskId: String = ""
)

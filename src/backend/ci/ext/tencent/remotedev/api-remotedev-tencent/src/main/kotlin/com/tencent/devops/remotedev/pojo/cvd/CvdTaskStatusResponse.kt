package com.tencent.devops.remotedev.pojo.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD任务状态响应")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdTaskStatusResponse(
    @get:Schema(description = "任务ID")
    val taskId: String,
    @get:Schema(
        description = "任务类型(create/revoke)"
    )
    val action: String,
    @get:Schema(
        description = "任务状态(waiting/running/succeeded/failed)"
    )
    val status: String,
    @get:Schema(description = "云桌面实例ID")
    val instanceId: String? = null,
    @get:Schema(description = "额外信息")
    val msg: String? = null
)

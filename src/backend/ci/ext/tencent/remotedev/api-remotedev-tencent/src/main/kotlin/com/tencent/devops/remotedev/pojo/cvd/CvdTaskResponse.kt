package com.tencent.devops.remotedev.pojo.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD领用/退回任务响应")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdTaskResponse(
    @get:Schema(description = "任务创建状态(success/failed)")
    val status: String,
    @get:Schema(description = "任务ID")
    val taskId: String,
    @get:Schema(description = "详情")
    val msg: String? = null
)

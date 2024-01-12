package com.tencent.devops.notify.pojo.messageTemplate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "语音模板信息")
data class VoiceTemplate(
    @Schema(description = "模板ID", required = true)
    val id: String,
    @Schema(description = "任务名称", required = false)
    var taskName: String,
    @Schema(description = "内容", required = true)
    var content: String
)

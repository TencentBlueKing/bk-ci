package com.tencent.devops.notify.pojo.messageTemplate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "语音模板信息")
data class VoiceTemplate(
    @Schema(title = "模板ID", required = true)
    val id: String,
    @Schema(title = "任务名称", required = false)
    var taskName: String,
    @Schema(title = "内容", required = true)
    var content: String
)

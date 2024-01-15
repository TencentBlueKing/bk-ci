package com.tencent.devops.notify.pojo.messageTemplate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "语音模板信息")
data class VoiceTemplate(
    @Schema(name = "模板ID", required = true)
    val id: String,
    @Schema(name = "任务名称", required = false)
    var taskName: String,
    @Schema(name = "内容", required = true)
    var content: String
)

package com.tencent.devops.notify.pojo.messageTemplate

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("语音模板信息")
data class VoiceTemplate(
    @ApiModelProperty("模板ID", required = true)
    val id: String,
    @ApiModelProperty("任务名称", required = false)
    var taskName: String,
    @ApiModelProperty("内容", required = true)
    var content: String
)

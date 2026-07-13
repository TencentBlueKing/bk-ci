package com.tencent.devops.process.pojo.trigger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线触发事件原因详情-校验单条明细")
data class PipelineTriggerValidateDetail(
    @get:Schema(title = "i18n 消息码")
    val messageCode: String,
    @get:Schema(title = "i18n 参数")
    val params: List<String>? = null
)

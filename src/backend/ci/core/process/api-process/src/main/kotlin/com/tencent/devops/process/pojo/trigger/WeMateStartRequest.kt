package com.tencent.devops.process.pojo.trigger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "weMate消息提醒启动请求")
data class WeMateStartRequest(
    @get:Schema(title = "触发用户", required = true)
    val triggerUser: String,
    @get:Schema(title = "消息内容", required = true)
    val message: String,
    @get:Schema(title = "流水线启动参数")
    val startParams: Map<String, String>? = null
)

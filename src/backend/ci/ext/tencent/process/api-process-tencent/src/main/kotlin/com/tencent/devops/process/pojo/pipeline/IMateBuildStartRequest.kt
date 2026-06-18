package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "IMate消息提醒启动请求")
data class IMateBuildStartRequest(
    @get:Schema(title = "触发用户", required = true)
    val triggerUser: String,
    @get:Schema(title = "消息内容", required = true)
    val message: String,
    @get:Schema(title = "流水线启动参数")
    val startParams: Map<String, String>? = null
)

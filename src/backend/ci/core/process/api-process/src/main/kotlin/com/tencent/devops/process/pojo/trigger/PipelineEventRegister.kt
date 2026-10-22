package com.tencent.devops.process.pojo.trigger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "外部事件注册记录")
data class PipelineEventRegister(
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "事件编码")
    val eventCode: String,
    @get:Schema(title = "事件源")
    val eventSource: String,
    @get:Schema(title = "事件类型")
    val eventType: String,
    @get:Schema(title = "事件作用域")
    val eventScope: String? = null,
    @get:Schema(title = "回调地址")
    val callbackUrl: String? = null,
    @get:Schema(title = "外部webhook ID")
    val externalId: String? = null
)

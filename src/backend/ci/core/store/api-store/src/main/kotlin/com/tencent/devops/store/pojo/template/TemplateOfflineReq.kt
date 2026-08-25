package com.tencent.devops.store.pojo.template

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板下架请求体")
data class TemplateOfflineReq(
    @get:Schema(title = "模板版本")
    val version: Long?,
    @get:Schema(title = "下架原因")
    val reason: String?
)

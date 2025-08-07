package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

data class TemplateVersionPair(
    @get:Schema(title = "模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "模板配置/资源版本", required = true)
    val version: Int
)

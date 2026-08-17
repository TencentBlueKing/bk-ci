package com.tencent.devops.process.pojo.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "上报插件外部链接请求")
data class ExternalLinkReportRequest(
    @get:Schema(title = "外部链接（仅支持 http/https）", required = true)
    val link: String
)

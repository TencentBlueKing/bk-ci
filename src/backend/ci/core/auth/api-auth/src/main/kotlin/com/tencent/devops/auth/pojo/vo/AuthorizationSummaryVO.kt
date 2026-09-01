package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "授权概况")
data class AuthorizationSummaryVO(
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "资源类型名称", required = true)
    val resourceTypeName: String,
    @get:Schema(title = "授权数量", required = true)
    val count: Long
)

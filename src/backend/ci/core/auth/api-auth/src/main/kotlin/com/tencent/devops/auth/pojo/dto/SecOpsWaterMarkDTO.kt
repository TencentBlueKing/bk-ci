package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "安全水印")
data class SecOpsWaterMarkDTO(
    @get:Schema(title = "场景token")
    val token: String,
    @get:Schema(title = "用户名称")
    val username: String
)

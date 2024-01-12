package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "安全水印")
data class SecOpsWaterMarkDTO(
    @Schema(description = "场景token")
    val token: String,
    @Schema(description = "用户名称")
    val username: String
)

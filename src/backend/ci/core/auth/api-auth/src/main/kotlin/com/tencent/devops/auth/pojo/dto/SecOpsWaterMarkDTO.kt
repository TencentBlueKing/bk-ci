package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "安全水印")
data class SecOpsWaterMarkDTO(
    @Schema(name = "场景token")
    val token: String,
    @Schema(name = "用户名称")
    val username: String
)

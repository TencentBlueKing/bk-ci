package com.tencent.devops.experience.pojo.outer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "外部登录-入参")
data class OuterLoginParam(
    @Schema(description = "用户名")
    val username: String,
    @Schema(description = "密码")
    val password: String
)

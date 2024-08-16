package com.tencent.devops.experience.pojo.outer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "外部登录-入参")
data class OuterLoginParam(
    @get:Schema(title = "用户名")
    val username: String,
    @get:Schema(title = "密码")
    val password: String
)

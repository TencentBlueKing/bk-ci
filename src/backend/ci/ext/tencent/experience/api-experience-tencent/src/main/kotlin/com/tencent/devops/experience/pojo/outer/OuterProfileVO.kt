package com.tencent.devops.experience.pojo.outer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "外部登录用户信息")
data class OuterProfileVO(
    @Schema(title = "用户名")
    val username: String,
    @Schema(title = "头像")
    val logo: String,
    @Schema(title = "邮箱")
    val email: String,
    @Schema(title = "类型,1--蓝鲸外部用户,2--太湖账户")
    val type: Int = 1
)

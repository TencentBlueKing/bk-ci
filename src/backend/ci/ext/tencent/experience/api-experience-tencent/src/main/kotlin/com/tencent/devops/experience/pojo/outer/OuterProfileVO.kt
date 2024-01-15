package com.tencent.devops.experience.pojo.outer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "外部登录用户信息")
data class OuterProfileVO(
    @Schema(description = "用户名")
    val username: String,
    @Schema(description = "头像")
    val logo: String,
    @Schema(description = "邮箱")
    val email: String,
    @Schema(description = "类型,1--蓝鲸外部用户,2--太湖账户")
    val type: Int = 1
)

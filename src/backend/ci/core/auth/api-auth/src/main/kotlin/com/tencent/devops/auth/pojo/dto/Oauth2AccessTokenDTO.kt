package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "oauth2获取token中间处理态")
data class Oauth2AccessTokenDTO(
    @Schema(description = "accessToken", required = true)
    val accessToken: String? = null,
    @Schema(description = "refreshToken", required = true)
    val refreshToken: String? = null,
    @Schema(description = "accessToken过期时间", required = true)
    val expiredTime: Long? = null,
    @Schema(description = "accessToken绑定的用户名称", required = true)
    val userName: String? = null,
    @Schema(description = "授权范围Id", required = true)
    val scopeId: Int
)

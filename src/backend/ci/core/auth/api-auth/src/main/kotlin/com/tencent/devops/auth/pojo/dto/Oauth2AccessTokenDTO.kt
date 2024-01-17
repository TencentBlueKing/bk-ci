package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "oauth2获取token中间处理态")
data class Oauth2AccessTokenDTO(
    @Schema(title = "accessToken", required = true)
    val accessToken: String? = null,
    @Schema(title = "refreshToken", required = true)
    val refreshToken: String? = null,
    @Schema(title = "accessToken过期时间", required = true)
    val expiredTime: Long? = null,
    @Schema(title = "accessToken绑定的用户名称", required = true)
    val userName: String? = null,
    @Schema(title = "授权范围Id", required = true)
    val scopeId: Int
)

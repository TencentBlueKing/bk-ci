package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "oauth2获取token请求返回体")
data class Oauth2AccessTokenVo(
    @Schema(description = "accessToken", required = true)
    val accessToken: String,
    @Schema(description = "accessToken过期时间", required = true)
    val expiredTime: Long,
    @Schema(description = "refreshToken", required = true)
    val refreshToken: String? = null
)

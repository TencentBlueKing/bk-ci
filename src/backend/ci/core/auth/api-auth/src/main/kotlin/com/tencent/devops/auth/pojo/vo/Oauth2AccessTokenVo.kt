package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "oauth2获取token请求返回体")
data class Oauth2AccessTokenVo(
    @Schema(name = "accessToken", required = true)
    val accessToken: String,
    @Schema(name = "accessToken过期时间", required = true)
    val expiredTime: Long,
    @Schema(name = "refreshToken", required = true)
    val refreshToken: String? = null
)

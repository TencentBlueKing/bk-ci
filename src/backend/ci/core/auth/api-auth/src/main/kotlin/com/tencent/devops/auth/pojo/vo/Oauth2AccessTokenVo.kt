package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "oauth2获取token请求返回体")
data class Oauth2AccessTokenVo(
    @get:Schema(title = "accessToken", required = true)
    val accessToken: String,
    @get:Schema(title = "accessToken过期时间", required = true)
    val expiredTime: Long,
    @get:Schema(title = "refreshToken", required = true)
    val refreshToken: String? = null
)

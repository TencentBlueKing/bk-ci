package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "oauth2获取token请求报文体")
data class Oauth2AccessTokenRequest(
    @Schema(title = "授权类型", required = true)
    val grantType: String,
    @Schema(title = "授权码,用于授权码模式", required = false)
    val code: String? = null,
    @Schema(title = "refreshToken,用于刷新授权码模式", required = false)
    val refreshToken: String? = null
)

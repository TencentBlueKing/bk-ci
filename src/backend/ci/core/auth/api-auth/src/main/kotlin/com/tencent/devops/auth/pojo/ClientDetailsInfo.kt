package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Oauth2客户端详情")
data class ClientDetailsInfo(
    @Schema(title = "客户端id", required = true)
    val clientId: String,
    @Schema(title = "客户端密钥", required = true)
    val clientSecret: String,
    @Schema(title = "客户端名称", required = true)
    val clientName: String,
    @Schema(title = "授权类型", required = true)
    val authorizedGrantTypes: String,
    @Schema(title = "跳转链接", required = true)
    val redirectUri: String,
    @Schema(title = "授权范围", required = true)
    val scope: String,
    @Schema(title = "accessToken有效期", required = true)
    val accessTokenValidity: Long,
    @Schema(title = "refreshToken有效期", required = true)
    val refreshTokenValidity: Long,
    @Schema(title = "图标", required = true)
    val icon: String
)

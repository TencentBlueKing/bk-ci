package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Oauth2客户端详情")
data class ClientDetailsInfo(
    @Schema(name = "客户端id", required = true)
    val clientId: String,
    @Schema(name = "客户端密钥", required = true)
    val clientSecret: String,
    @Schema(name = "客户端名称", required = true)
    val clientName: String,
    @Schema(name = "授权类型", required = true)
    val authorizedGrantTypes: String,
    @Schema(name = "跳转链接", required = true)
    val redirectUri: String,
    @Schema(name = "授权范围", required = true)
    val scope: String,
    @Schema(name = "accessToken有效期", required = true)
    val accessTokenValidity: Long,
    @Schema(name = "refreshToken有效期", required = true)
    val refreshTokenValidity: Long,
    @Schema(name = "图标", required = true)
    val icon: String
)

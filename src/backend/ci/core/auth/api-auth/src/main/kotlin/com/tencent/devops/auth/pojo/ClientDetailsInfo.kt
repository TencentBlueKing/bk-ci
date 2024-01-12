package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Oauth2客户端详情")
data class ClientDetailsInfo(
    @Schema(description = "客户端id", required = true)
    val clientId: String,
    @Schema(description = "客户端密钥", required = true)
    val clientSecret: String,
    @Schema(description = "客户端名称", required = true)
    val clientName: String,
    @Schema(description = "授权类型", required = true)
    val authorizedGrantTypes: String,
    @Schema(description = "跳转链接", required = true)
    val redirectUri: String,
    @Schema(description = "授权范围", required = true)
    val scope: String,
    @Schema(description = "accessToken有效期", required = true)
    val accessTokenValidity: Long,
    @Schema(description = "refreshToken有效期", required = true)
    val refreshTokenValidity: Long,
    @Schema(description = "图标", required = true)
    val icon: String
)

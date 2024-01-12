package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Oauth2客户端请求实体")
data class ClientDetailsDTO(
    @Schema(description = "客户端ID")
    val clientId: String,
    @Schema(description = "客户端秘钥")
    val clientSecret: String,
    @Schema(description = "客户端名称")
    val clientName: String,
    @Schema(description = "授权操作范围")
    val scope: String,
    @Schema(description = "图标")
    val icon: String,
    @Schema(description = "授权模式")
    val authorizedGrantTypes: String,
    @Schema(description = "跳转链接")
    val webServerRedirectUri: String,
    @Schema(description = "access_token有效时间")
    val accessTokenValidity: Long,
    @Schema(description = "refresh_token有效时间")
    val refreshTokenValidity: Long,
    @Schema(description = "创建人")
    val createUser: String? = null,
    @Schema(description = "更新人")
    val updateUser: String? = null
)

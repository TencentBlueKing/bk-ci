package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Oauth2客户端请求实体")
data class ClientDetailsDTO(
    @Schema(title = "客户端ID")
    val clientId: String,
    @Schema(title = "客户端秘钥")
    val clientSecret: String,
    @Schema(title = "客户端名称")
    val clientName: String,
    @Schema(title = "授权操作范围")
    val scope: String,
    @Schema(title = "图标")
    val icon: String,
    @Schema(title = "授权模式")
    val authorizedGrantTypes: String,
    @Schema(title = "跳转链接")
    val webServerRedirectUri: String,
    @Schema(title = "access_token有效时间")
    val accessTokenValidity: Long,
    @Schema(title = "refresh_token有效时间")
    val refreshTokenValidity: Long,
    @Schema(title = "创建人")
    val createUser: String? = null,
    @Schema(title = "更新人")
    val updateUser: String? = null
)

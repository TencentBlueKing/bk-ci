package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Oauth2客户端请求实体")
data class ClientDetailsDTO(
    @Schema(name = "客户端ID")
    val clientId: String,
    @Schema(name = "客户端秘钥")
    val clientSecret: String,
    @Schema(name = "客户端名称")
    val clientName: String,
    @Schema(name = "授权操作范围")
    val scope: String,
    @Schema(name = "图标")
    val icon: String,
    @Schema(name = "授权模式")
    val authorizedGrantTypes: String,
    @Schema(name = "跳转链接")
    val webServerRedirectUri: String,
    @Schema(name = "access_token有效时间")
    val accessTokenValidity: Long,
    @Schema(name = "refresh_token有效时间")
    val refreshTokenValidity: Long,
    @Schema(name = "创建人")
    val createUser: String? = null,
    @Schema(name = "更新人")
    val updateUser: String? = null
)

package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Oauth2客户端请求实体")
data class ClientDetailsDTO(
    @get:Schema(title = "客户端ID")
    val clientId: String,
    @get:Schema(title = "客户端秘钥")
    val clientSecret: String,
    @get:Schema(title = "客户端名称")
    val clientName: String,
    @get:Schema(title = "授权操作范围")
    val scope: String,
    @get:Schema(title = "图标")
    val icon: String,
    @get:Schema(title = "授权模式")
    val authorizedGrantTypes: List<Oauth2GrantType>,
    @get:Schema(title = "跳转链接")
    val webServerRedirectUri: String,
    @get:Schema(title = "access_token有效时间")
    val accessTokenValidity: Long,
    @get:Schema(title = "refresh_token有效时间")
    val refreshTokenValidity: Long,
    @get:Schema(title = "创建人")
    val createUser: String = "system",
    @get:Schema(title = "更新人")
    val updateUser: String = "system"
)

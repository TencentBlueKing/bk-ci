package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Oauth2客户端请求实体")
data class ClientDetailsDTO(
    @ApiModelProperty("客户端ID")
    val clientId: String,
    @ApiModelProperty("客户端秘钥")
    val clientSecret: String,
    @ApiModelProperty("客户端名称")
    val clientName: String,
    @ApiModelProperty("授权操作范围")
    val scope: String,
    @ApiModelProperty("图标")
    val icon: String,
    @ApiModelProperty("授权模式")
    val authorizedGrantTypes: String,
    @ApiModelProperty("跳转链接")
    val webServerRedirectUri: String,
    @ApiModelProperty("access_token有效时间")
    val accessTokenValidity: Long,
    @ApiModelProperty("refresh_token有效时间")
    val refreshTokenValidity: Long,
    @ApiModelProperty("创建人")
    val createUser: String? = null,
    @ApiModelProperty("更新人")
    val updateUser: String? = null
)

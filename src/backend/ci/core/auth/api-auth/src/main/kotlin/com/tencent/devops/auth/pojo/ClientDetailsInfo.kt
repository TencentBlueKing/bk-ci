package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Oauth2客户端详情")
data class ClientDetailsInfo(
    @ApiModelProperty("客户端id", required = true)
    val clientId: String,
    @ApiModelProperty("客户端密钥", required = true)
    val clientSecret: String,
    @ApiModelProperty("客户端名称", required = true)
    val clientName: String,
    @ApiModelProperty("授权类型", required = true)
    val authorizedGrantTypes: String,
    @ApiModelProperty("跳转链接", required = true)
    val redirectUri: String,
    @ApiModelProperty("授权范围", required = true)
    val scope: String,
    @ApiModelProperty("accessToken有效期", required = true)
    val accessTokenValidity: Long,
    @ApiModelProperty("refreshToken有效期", required = true)
    val refreshTokenValidity: Long
)

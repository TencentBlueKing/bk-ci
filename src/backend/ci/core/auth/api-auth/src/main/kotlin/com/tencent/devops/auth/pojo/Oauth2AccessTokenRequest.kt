package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("oauth2获取token请求报文体")
data class Oauth2AccessTokenRequest(
    @ApiModelProperty("授权类型", required = true)
    val grantType: String,
    @ApiModelProperty("授权码,用于授权码模式", required = false)
    val code: String? = null,
    @ApiModelProperty("refreshToken,用于刷新授权码模式", required = false)
    val refreshToken: String? = null
)

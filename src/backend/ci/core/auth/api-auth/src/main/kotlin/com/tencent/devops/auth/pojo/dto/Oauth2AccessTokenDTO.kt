package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import javax.print.attribute.standard.JobOriginatingUserName

@ApiModel("oauth2获取token请求报文体")
data class Oauth2AccessTokenDTO(
    @ApiModelProperty("客户端id", required = true)
    val clientId: String,
    @ApiModelProperty("用户名称", required = true)
    val userName: String = "",
    @ApiModelProperty("客户端密钥", required = true)
    val clientSecret: String,
    @ApiModelProperty("授权类型", required = true)
    val grantType: String,
    @ApiModelProperty("授权码,用于授权码模式", required = false)
    val code: String? = null,
    @ApiModelProperty("refreshToken,用于刷新授权码模式", required = false)
    val refreshToken: String? = null
)

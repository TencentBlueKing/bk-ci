package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("oauth2获取token返回报文体")
data class Oauth2AccessTokenVo(
    @ApiModelProperty("accessToken", required = true)
    val accessToken: String,
    @ApiModelProperty("accessToken过期时间", required = true)
    val expiredTime: Long,
    @ApiModelProperty("refreshToken", required = true)
    val refreshToken: String? = null
)

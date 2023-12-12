package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("oauth2获取token中间处理态")
data class Oauth2AccessTokenDTO(
    @ApiModelProperty("accessToken", required = true)
    val accessToken: String? = null,
    @ApiModelProperty("refreshToken", required = true)
    val refreshToken: String? = null,
    @ApiModelProperty("accessToken过期时间", required = true)
    val expiredTime: Long? = null,
    @ApiModelProperty("accessToken绑定的用户名称", required = true)
    val userName: String? = null,
    @ApiModelProperty("授权范围Id", required = true)
    val scopeId: Int
)

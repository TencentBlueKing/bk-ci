package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("oauth2获取token中间处理态")
data class Oauth2AccessTokenInfo(
    @ApiModelProperty("accessToken", required = true)
    var accessToken: String? = null,
    @ApiModelProperty("refreshToken", required = true)
    var refreshToken: String? = null,
    @ApiModelProperty("accessToken过期时间", required = true)
    var expiredTime: Int? = null
)

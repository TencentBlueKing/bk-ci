package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("oauth2获取授权码请求报文体")
data class Oauth2AuthorizationCodeDTO(
    @ApiModelProperty("授权范围", required = true)
    val scope: List<String>
)

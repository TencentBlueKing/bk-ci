package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("oauth2获取授权信息请求返回体")
data class Oauth2AuthorizationInfoVo(
    @ApiModelProperty("用户名称", required = true)
    val userName: String,
    @ApiModelProperty("客户端名称", required = true)
    val clientName: String,
    @ApiModelProperty("授权范围", required = true)
    val scope: Map<String/*授权范围Id*/, String/*授权范围名称*/>
)

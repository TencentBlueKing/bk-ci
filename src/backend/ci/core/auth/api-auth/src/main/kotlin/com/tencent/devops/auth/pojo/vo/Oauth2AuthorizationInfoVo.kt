package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "oauth2获取授权信息请求返回体")
data class Oauth2AuthorizationInfoVo(
    @Schema(title = "用户名称", required = true)
    val userName: String,
    @Schema(title = "客户端名称", required = true)
    val clientName: String,
    @Schema(title = "图标", required = true)
    val icon: String,
    @Schema(title = "授权范围", required = true)
    val scope: Map<String/*授权范围Id*/, String/*授权范围名称*/>
)

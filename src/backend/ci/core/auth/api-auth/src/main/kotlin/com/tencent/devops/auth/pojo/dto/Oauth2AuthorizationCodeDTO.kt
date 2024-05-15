package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "oauth2获取授权码请求报文体")
data class Oauth2AuthorizationCodeDTO(
    @get:Schema(title = "授权范围", required = true)
    val scope: List<String>
)

package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "oauth2获取授权码请求报文体")
data class Oauth2AuthorizationCodeDTO(
    @Schema(description = "授权范围", required = true)
    val scope: List<String>
)

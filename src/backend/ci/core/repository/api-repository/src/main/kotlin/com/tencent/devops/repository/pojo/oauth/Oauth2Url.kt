package com.tencent.devops.repository.pojo.oauth

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "重置Oauth授权信息")
data class Oauth2Url(
    val url: String
)

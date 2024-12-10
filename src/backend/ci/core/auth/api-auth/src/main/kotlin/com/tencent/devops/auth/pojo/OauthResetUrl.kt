package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "重置Oauth授权信息")
data class OauthResetUrl(
    val url: String
)

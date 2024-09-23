package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户Oauth授权信息")
data class OauthRepository(
    @get:Schema(title = "代码库别名")
    val aliasName: String,
    @get:Schema(title = "蓝盾代码库链接")
    val url: String
)

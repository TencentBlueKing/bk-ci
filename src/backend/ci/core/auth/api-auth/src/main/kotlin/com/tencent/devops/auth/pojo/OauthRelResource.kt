package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户Oauth关联的资源")
data class OauthRelResource(
    @get:Schema(title = "资源名称")
    val name: String,
    @get:Schema(title = "资源链接")
    val url: String
)

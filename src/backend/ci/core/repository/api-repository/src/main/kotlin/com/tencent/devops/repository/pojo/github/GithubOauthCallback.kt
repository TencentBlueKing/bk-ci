package com.tencent.devops.repository.pojo.github

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "github oauth回调后信息")
data class GithubOauthCallback(
    val userId: String,
    val email: String? = "",
    @get:Schema(title = "回调后跳转的界面")
    val redirectUrl: String
)

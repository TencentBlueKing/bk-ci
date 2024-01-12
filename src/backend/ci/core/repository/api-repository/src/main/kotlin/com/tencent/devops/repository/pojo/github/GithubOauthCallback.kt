package com.tencent.devops.repository.pojo.github

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "github oauth回调后信息")
data class GithubOauthCallback(
    val userId: String,
    val email: String? = "",
    @Schema(description = "回调后跳转的界面")
    val redirectUrl: String
)

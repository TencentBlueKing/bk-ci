package com.tencent.devops.repository.pojo.github

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("github oauth回调后信息")
data class GithubOauthCallback(
    val userId: String,
    val email: String? = "",
    @ApiModelProperty("回调后跳转的界面")
    val redirectUrl: String
)

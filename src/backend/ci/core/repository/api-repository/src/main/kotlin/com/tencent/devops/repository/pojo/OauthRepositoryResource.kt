package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Oauth授权类型的代码库资源信息")
data class OauthRepositoryResource(
    @get:Schema(title = "资源名称")
    val name: String,
    @get:Schema(title = "资源链接")
    val url: String
)

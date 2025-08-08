package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "代码库授权凭证类型展示")
data class RepoCredentialTypeVo(
    @get:Schema(title = "凭证类型", required = true)
    val credentialType: String,
    @get:Schema(title = "凭证类型名", required = true)
    val name: String,
    @get:Schema(title = "授权类型", required = true)
    val authType: String
)

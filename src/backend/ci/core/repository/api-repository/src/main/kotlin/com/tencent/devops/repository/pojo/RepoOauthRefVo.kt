package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户关联的仓库")
data class RepoOauthRefVo(
    @get:Schema(title = "仓库别名", required = true)
    val aliasName: String,
    @get:Schema(title = "URL", required = true)
    val url: String
)

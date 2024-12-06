package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户关联的仓库")
data class RepoOauthRefVo(
    @get:Schema(title = "仓库别名", required = true)
    val aliasName: String,
    @get:Schema(title = "仓库源URL", required = true)
    val url: String,
    @get:Schema(title = "代码库详情页", required = true)
    var detailUrl: String? = null,
    @get:Schema(title = "蓝盾项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "代码库HashId", required = true)
    val hashId: String
)

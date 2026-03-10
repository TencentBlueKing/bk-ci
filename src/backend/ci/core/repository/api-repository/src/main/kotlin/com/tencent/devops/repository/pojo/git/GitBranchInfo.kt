package com.tencent.devops.repository.pojo.git

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "工蜂分支基础信息")
data class GitBranchInfo(
    @get:Schema(title = "分支名称", required = true)
    val name: String,
    @get:Schema(title = "最新版本", required = true)
    val commitId: String,
    @get:Schema(title = "是否为默认分支", required = true)
    val default: Boolean = false
)
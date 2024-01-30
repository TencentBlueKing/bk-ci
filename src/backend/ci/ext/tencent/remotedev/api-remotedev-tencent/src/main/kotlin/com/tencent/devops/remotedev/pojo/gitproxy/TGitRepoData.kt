package com.tencent.devops.remotedev.pojo.gitproxy

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "关联的工蜂仓库的信息")
data class TGitRepoData(
    @get:Schema(title = "仓库URL")
    val url: String,
    @get:Schema(title = "关联状态 TO_BE_MIGRATED|AVAILABLE|ABNORMAL")
    val status: TGitRepoStatus
)

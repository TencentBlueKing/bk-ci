package com.tencent.devops.stream.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "仓库中所有已构建的分支列表")
data class StreamBuildBranch(
    @get:Schema(title = "分支名")
    val branchName: String,
    @get:Schema(title = "项目ID")
    val gitProjectId: Long,
    @get:Schema(title = "源项目ID")
    val sourceGitProjectId: Long?
)

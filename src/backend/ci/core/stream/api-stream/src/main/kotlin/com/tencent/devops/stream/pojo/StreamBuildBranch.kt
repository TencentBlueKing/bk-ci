package com.tencent.devops.stream.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "仓库中所有已构建的分支列表")
data class StreamBuildBranch(
    @Schema(name = "分支名")
    val branchName: String,
    @Schema(name = "项目ID")
    val gitProjectId: Long,
    @Schema(name = "源项目ID")
    val sourceGitProjectId: Long?
)

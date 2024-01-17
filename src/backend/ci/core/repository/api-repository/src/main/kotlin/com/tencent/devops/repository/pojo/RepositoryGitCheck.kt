package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "git check 信息返回模型")
data class RepositoryGitCheck(
    @Schema(title = "git check id")
    val gitCheckId: Long,
    @Schema(title = "流水线id")
    val pipelineId: String,
    @Schema(title = "构建次数")
    val buildNumber: Int,
    @Schema(title = "仓库id")
    val repositoryId: String?,
    @Schema(title = "仓库名称")
    val repositoryName: String?,
    @Schema(title = "提交id")
    val commitId: String,
    @Schema(title = "内容")
    val context: String,
    @Schema(title = "来源类型")
    val source: ExecuteSource,
    @Schema(title = "目标分支")
    val targetBranch: String? = ""
)

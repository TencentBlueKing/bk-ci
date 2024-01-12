package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "git check 信息返回模型")
data class RepositoryGitCheck(
    @Schema(description = "git check id")
    val gitCheckId: Long,
    @Schema(description = "流水线id")
    val pipelineId: String,
    @Schema(description = "构建次数")
    val buildNumber: Int,
    @Schema(description = "仓库id")
    val repositoryId: String?,
    @Schema(description = "仓库名称")
    val repositoryName: String?,
    @Schema(description = "提交id")
    val commitId: String,
    @Schema(description = "内容")
    val context: String,
    @Schema(description = "来源类型")
    val source: ExecuteSource,
    @Schema(description = "目标分支")
    val targetBranch: String? = ""
)

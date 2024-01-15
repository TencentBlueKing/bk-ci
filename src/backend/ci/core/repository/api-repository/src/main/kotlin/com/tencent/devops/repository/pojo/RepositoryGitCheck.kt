package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "git check 信息返回模型")
data class RepositoryGitCheck(
    @Schema(name = "git check id")
    val gitCheckId: Long,
    @Schema(name = "流水线id")
    val pipelineId: String,
    @Schema(name = "构建次数")
    val buildNumber: Int,
    @Schema(name = "仓库id")
    val repositoryId: String?,
    @Schema(name = "仓库名称")
    val repositoryName: String?,
    @Schema(name = "提交id")
    val commitId: String,
    @Schema(name = "内容")
    val context: String,
    @Schema(name = "来源类型")
    val source: ExecuteSource,
    @Schema(name = "目标分支")
    val targetBranch: String? = ""
)

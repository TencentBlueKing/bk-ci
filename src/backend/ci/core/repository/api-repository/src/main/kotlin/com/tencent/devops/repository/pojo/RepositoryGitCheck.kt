package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "git check 信息返回模型")
data class RepositoryGitCheck(
    @get:Schema(title = "git check id")
    val gitCheckId: Long,
    @get:Schema(title = "流水线id")
    val pipelineId: String,
    @get:Schema(title = "构建次数")
    val buildNumber: Int,
    @get:Schema(title = "仓库id")
    val repositoryId: String?,
    @get:Schema(title = "仓库名称")
    val repositoryName: String?,
    @get:Schema(title = "提交id")
    val commitId: String,
    @get:Schema(title = "内容")
    val context: String,
    @get:Schema(title = "来源类型")
    val source: ExecuteSource,
    @get:Schema(title = "目标分支")
    val targetBranch: String? = ""
)

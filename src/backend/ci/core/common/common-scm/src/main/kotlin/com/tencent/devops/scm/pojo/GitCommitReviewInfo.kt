package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "日常代码评审")
data class GitCommitReviewInfo constructor(
    val id: String,
    @Schema(description = "标签")
    val labels: List<String>?,
    @Schema(description = "标题")
    val title: String,
    @Schema(description = "描述")
    val description: String?,
    @Schema(description = "提交信息")
    val commits: List<GitCrCommit>? = listOf(),
    @Schema(description = "状态")
    val state: String
)

@Schema(description = "日常代码评审--提交信息")
data class GitCrCommit constructor(
    @Schema(description = "提交时间")
    @JsonProperty("commit_date")
    val commitDate: String,
    @Schema(description = "commitId")
    @JsonProperty("id")
    val id: String
)

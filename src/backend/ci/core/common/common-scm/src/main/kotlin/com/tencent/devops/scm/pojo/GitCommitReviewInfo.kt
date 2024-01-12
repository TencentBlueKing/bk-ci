package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "日常代码评审")
data class GitCommitReviewInfo constructor(
    val id: String,
    @Schema(name = "标签")
    val labels: List<String>?,
    @Schema(name = "标题")
    val title: String,
    @Schema(name = "描述")
    val description: String?,
    @Schema(name = "提交信息")
    val commits: List<GitCrCommit>? = listOf(),
    @Schema(name = "状态")
    val state: String
)

@Schema(name = "日常代码评审--提交信息")
data class GitCrCommit constructor(
    @Schema(name = "提交时间")
    @JsonProperty("commit_date")
    val commitDate: String,
    @Schema(name = "commitId")
    @JsonProperty("id")
    val id: String
)

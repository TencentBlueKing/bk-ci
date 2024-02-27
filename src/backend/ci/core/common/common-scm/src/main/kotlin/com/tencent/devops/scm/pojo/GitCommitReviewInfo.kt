package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "日常代码评审")
data class GitCommitReviewInfo constructor(
    val id: String,
    @get:Schema(title = "标签")
    val labels: List<String>?,
    @get:Schema(title = "标题")
    val title: String,
    @get:Schema(title = "描述")
    val description: String?,
    @get:Schema(title = "提交信息")
    val commits: List<GitCrCommit>? = listOf(),
    @get:Schema(title = "状态")
    val state: String
)

@Schema(title = "日常代码评审--提交信息")
data class GitCrCommit constructor(
    @get:Schema(title = "提交时间")
    @JsonProperty("commit_date")
    val commitDate: String,
    @get:Schema(title = "commitId")
    @JsonProperty("id")
    val id: String
)

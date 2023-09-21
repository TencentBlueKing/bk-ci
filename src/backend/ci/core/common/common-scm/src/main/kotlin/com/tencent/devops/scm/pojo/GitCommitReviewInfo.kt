package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("日常代码评审")
data class GitCommitReviewInfo constructor(
    val id: String,
    @ApiModelProperty("标签")
    val labels: List<String>?,
    @ApiModelProperty("标题")
    val title: String,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("提交信息")
    val commits: List<GitCrCommit>? = listOf(),
    @ApiModelProperty("状态")
    val state: String
)

@ApiModel("日常代码评审--提交信息")
data class GitCrCommit constructor(
    @ApiModelProperty("提交时间")
    @JsonProperty("commit_date")
    val commitDate: String,
    @ApiModelProperty("commitId")
    @JsonProperty("id")
    val id: String
)

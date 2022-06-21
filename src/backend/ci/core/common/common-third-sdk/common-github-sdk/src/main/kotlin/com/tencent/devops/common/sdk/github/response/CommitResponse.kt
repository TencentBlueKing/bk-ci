package com.tencent.devops.common.sdk.github.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.github.pojo.CommitFile
import com.tencent.devops.common.sdk.github.pojo.CommitStats
import com.tencent.devops.common.sdk.github.pojo.GHBranchCommit
import com.tencent.devops.common.sdk.github.pojo.GithubAuthor
import com.tencent.devops.common.sdk.github.pojo.GithubCommit

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommitResponse(
    val url: String,
    val sha: String,
    @JsonProperty("node_id")
    val nodeId: String,
    @JsonProperty("html_url")
    val htmlUrl: String,
    @JsonProperty("comments_url")
    val commentsUrl: String,
    val commit: GithubCommit,
    val author: GithubAuthor,
    val committer: GithubAuthor,
    val parents: List<GHBranchCommit>,
    val stats: CommitStats? = null,
    val files: List<CommitFile>? = null
)

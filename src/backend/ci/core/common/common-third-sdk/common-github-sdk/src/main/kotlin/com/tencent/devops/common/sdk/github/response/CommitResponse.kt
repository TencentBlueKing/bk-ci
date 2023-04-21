package com.tencent.devops.common.sdk.github.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.github.pojo.GithubCommit
import com.tencent.devops.common.sdk.github.pojo.GithubCommitFile
import com.tencent.devops.common.sdk.github.pojo.GithubSha
import com.tencent.devops.common.sdk.github.pojo.GithubUser

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
    val author: GithubUser?,
    val committer: GithubUser?,
    val parents: List<GithubSha>,
    val files: List<GithubCommitFile>?
)

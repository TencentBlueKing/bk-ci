package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GithubCommit(
    val url: String,
    val author: GithubCommitAuthor,
    val committer: GithubCommitAuthor,
    val message: String,
    val tree: GHBranchCommit,
    @JsonProperty("comment_count")
    val commentCount: Int,
    val verification: Verification
)

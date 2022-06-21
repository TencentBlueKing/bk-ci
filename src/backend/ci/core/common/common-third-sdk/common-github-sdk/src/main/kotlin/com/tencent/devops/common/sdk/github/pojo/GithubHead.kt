package com.tencent.devops.common.sdk.github.pojo

data class GithubHead(
    val label: String,
    val ref: String,
    val sha: String,
    val user: GithubAuthor,
    val repo: GithubRepo
)

package com.tencent.devops.repository.sdk.github.pojo

data class GithubCommitPointer(
    val label: String,
    val ref: String,
    val sha: String,
    val user: GithubUser,
    val repo: GithubRepo
)

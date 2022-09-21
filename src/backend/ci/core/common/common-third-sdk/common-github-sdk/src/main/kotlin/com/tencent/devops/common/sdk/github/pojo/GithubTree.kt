package com.tencent.devops.common.sdk.github.pojo

data class GithubTree(
    val mode: String,
    val path: String,
    val sha: String,
    val size: Int,
    val type: String,
    val url: String?
)

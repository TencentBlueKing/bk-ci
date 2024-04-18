package com.tencent.devops.repository.sdk.github.response

import com.tencent.devops.repository.sdk.github.pojo.GithubTree

data class GetTreeResponse(
    val sha: String,
    val url: String,
    val tree: List<GithubTree>,
    val truncated: Boolean
)

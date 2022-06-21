package com.tencent.devops.common.sdk.github.pojo

import java.time.LocalDateTime

data class GithubCommitAuthor(
    val name: String,
    val nemail: String,
    val date: LocalDateTime
)

package com.tencent.devops.process.pojo.scm.code.github

data class GithubWebhook(
    val event: String,
    val guid: String,
    val signature: String,
    val body: String
)
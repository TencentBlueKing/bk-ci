package com.tencent.devops.remotedev.pojo.gitproxy

data class CreateGitProxyData(
    val gitType: String,
    val projectId: String,
    val url: String
)

enum class GitType {
    TGIT
}

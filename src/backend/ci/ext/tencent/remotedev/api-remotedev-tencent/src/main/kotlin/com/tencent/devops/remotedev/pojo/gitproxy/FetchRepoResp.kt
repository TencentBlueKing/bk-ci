package com.tencent.devops.remotedev.pojo.gitproxy

data class FetchRepoResp(
    val url: String,
    val proxyUrl: String?,
    val creator: String,
    val createdDate: String,
    val repoName: String,
    val type: String,
    val desc: String?,
    val lfsUrl: String?
)

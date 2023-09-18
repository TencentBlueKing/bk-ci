package com.tencent.devops.remotedev.pojo.gitproxy

data class FetchRepoResp(
    val url: String,
    val proxyUrl: String?,
    val creator: String,
    val creatDate: String
)

package com.tencent.devops.remotedev.pojo.gitproxy

data class RefreshCodeProxyData(
    val projectId: String,
    val name: String,
    val type: String,
    val url: String,
    val conf: CodeProxyConf,
    val desc: String?,
    val creator: String,
    val enableLfs: Boolean
)

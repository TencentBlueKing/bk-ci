package com.tencent.bkrepo.opdata.model

data class RepoInfo(
    val projectId: String,
    val name: String,
    val credentialsKey: String? = "default"
)

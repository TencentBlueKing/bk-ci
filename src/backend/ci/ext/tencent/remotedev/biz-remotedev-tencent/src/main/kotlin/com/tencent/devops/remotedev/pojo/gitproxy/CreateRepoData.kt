package com.tencent.devops.remotedev.pojo.gitproxy

data class CreateRepoData(
    val projectId: String,
    val name: String,
    val type: String,
    val category: String,
    val public: Boolean,
    val description: String?,
    val configuration: RepoConfig,
    val display: Boolean
)

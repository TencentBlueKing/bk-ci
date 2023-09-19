package com.tencent.devops.remotedev.pojo.gitproxy

data class RepoConfig(
    val type: String,
    val proxy: CreateRepoDataConfigProxy,
    val url: String?
)

data class CreateRepoDataConfigProxy(
    val public: Boolean,
    val name: String,
    val url: String
)
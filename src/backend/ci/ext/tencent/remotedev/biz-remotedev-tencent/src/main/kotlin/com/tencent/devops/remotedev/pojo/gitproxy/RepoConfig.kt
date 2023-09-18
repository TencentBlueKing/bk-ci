package com.tencent.devops.remotedev.pojo.gitproxy

data class RepoConfig(
    val type: String,
    val proxy: CreateRepoDataConfigProxy,
    val url: String?,
    val settings: Map<String, Any>,
    val webHook: CreateRepoDataConfigWebHook
)

data class CreateRepoDataConfigProxy(
    val public: Boolean,
    val name: String,
    val url: String,
    val credentialKey: String?,
    val username: String?,
    val password: String?
)

data class CreateRepoDataConfigWebHook(
    val webHookList: List<String>
)

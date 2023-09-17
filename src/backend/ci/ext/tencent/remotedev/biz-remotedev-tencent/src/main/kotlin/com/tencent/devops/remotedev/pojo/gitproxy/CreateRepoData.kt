package com.tencent.devops.remotedev.pojo.gitproxy

data class CreateRepoData(
    val projectId: String,
    val name: String,
    val type: String,
    val category: String,
    val public: Boolean,
    val description: String,
    val configuration: CreateRepoDataConfig,
    val storageCredentialsKey: String?
)

data class CreateRepoDataConfig(
    val type: String,
    val proxy: CreateRepoDataConfigProxy,
    val url: Any?,
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

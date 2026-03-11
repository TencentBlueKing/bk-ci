package com.tencent.devops.remotedev.pojo.gitproxy

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RepoConfig(
    val type: String,
    val proxy: CreateRepoDataConfigProxy?,
    val url: String?,
    val settings: CreateRepoRespDataConfSettings?
)

data class CreateRepoDataConfigProxy(
    val public: Boolean,
    val name: String,
    val url: String
)

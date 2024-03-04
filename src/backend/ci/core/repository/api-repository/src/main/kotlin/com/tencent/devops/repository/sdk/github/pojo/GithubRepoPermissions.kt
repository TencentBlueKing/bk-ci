package com.tencent.devops.repository.sdk.github.pojo

data class GithubRepoPermissions(
    val admin: Boolean,
    val pull: Boolean,
    val push: Boolean
)

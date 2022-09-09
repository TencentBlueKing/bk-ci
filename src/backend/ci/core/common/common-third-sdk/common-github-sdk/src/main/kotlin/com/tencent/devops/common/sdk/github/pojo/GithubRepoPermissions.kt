package com.tencent.devops.common.sdk.github.pojo

data class GithubRepoPermissions(
    val admin: Boolean,
    val pull: Boolean,
    val push: Boolean
)

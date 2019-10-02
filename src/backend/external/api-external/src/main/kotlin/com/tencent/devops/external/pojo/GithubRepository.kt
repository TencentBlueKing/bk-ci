package com.tencent.devops.external.pojo

data class GithubRepository(
    val id: String,
    val name: String,
    val fullName: String,
    val sshUrl: String,
    val httpUrl: String,
    val updatedAt: Long
)
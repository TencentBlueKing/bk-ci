package com.tencent.devops.external.pojo.github

import com.fasterxml.jackson.annotation.JsonProperty

data class GithubRepo(
    val id: Long,
    val name: String,
    @JsonProperty("full_name")
    val fullName: String,
    @JsonProperty("clone_url")
    val httpUrl: String,
    @JsonProperty("ssh_url")
    val sshUrl: String,
    @JsonProperty("updated_at")
    val updateAt: String
)
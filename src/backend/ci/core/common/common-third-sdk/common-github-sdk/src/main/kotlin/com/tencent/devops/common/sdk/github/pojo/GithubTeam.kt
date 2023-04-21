package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GithubTeam(
    val description: String,
    @JsonProperty("html_url")
    val htmlUrl: String,
    val id: Int,
    @JsonProperty("members_url")
    val membersUrl: String,
    val name: String,
    @JsonProperty("node_id")
    val nodeId: String,
    val permission: String,
    val privacy: String,
    @JsonProperty("repositories_url")
    val repositoriesUrl: String,
    val slug: String,
    val url: String
)

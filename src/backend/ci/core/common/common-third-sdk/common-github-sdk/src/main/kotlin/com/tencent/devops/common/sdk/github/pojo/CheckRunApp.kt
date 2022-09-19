package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class CheckRunApp(
    @JsonProperty("created_at")
    val createdAt: String,
    val description: String,
    val events: List<String>,
    @JsonProperty("external_url")
    val externalUrl: String,
    @JsonProperty("html_url")
    val htmlUrl: String,
    val id: Int,
    val name: String,
    @JsonProperty("node_id")
    val nodeId: String,
    val owner: GithubUser,
    val permissions: CheckRunAppPermissions,
    val slug: String,
    @JsonProperty("updated_at")
    val updatedAt: String
)

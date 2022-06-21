package com.tencent.devops.common.sdk.github.pojo


import com.fasterxml.jackson.annotation.JsonProperty

data class GHUserPlan(
    @JsonProperty("collaborators")
    val collaborators: Int,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("private_repos")
    val privateRepos: Int,
    @JsonProperty("space")
    val space: Int
)

package com.tencent.devops.external.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GithubToken(
    @JsonProperty("access_token")
    val accessToken: String,
    val scope: String,
    @JsonProperty("token_type")
    val tokenType: String
)
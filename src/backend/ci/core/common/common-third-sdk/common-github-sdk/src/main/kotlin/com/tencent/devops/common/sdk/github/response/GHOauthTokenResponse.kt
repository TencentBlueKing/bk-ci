package com.tencent.devops.common.sdk.github.response

import com.fasterxml.jackson.annotation.JsonProperty

data class GHOauthTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    val scope: String
)

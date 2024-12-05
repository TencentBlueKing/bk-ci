package com.tencent.devops.repository.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class CodeGitCopilotOauthResponse(
    val data: OauthInfo,
    val code: Int,
    val message: String,
    val timestamp: Long
)

data class OauthInfo(
    val username: String,
    @JsonProperty("app_id")
    val appId: String,
    @JsonProperty("access_token")
    val accessToken: String
)

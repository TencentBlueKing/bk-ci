package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.GHOauthTokenResponse

data class GHOauthTokenRequest (
    @JsonProperty("client_id")
    val clientId: String,
    @JsonProperty("client_secret")
    val clientSecret: String,
    val code: String,
    @JsonProperty("redirect_uri")
    val redirectUri: String?
) : GithubRequest<GHOauthTokenResponse>() {
    override fun getHttpMethod() = HttpMethod.POST

    override fun getApiPath() = "login/oauth/access_token"
}

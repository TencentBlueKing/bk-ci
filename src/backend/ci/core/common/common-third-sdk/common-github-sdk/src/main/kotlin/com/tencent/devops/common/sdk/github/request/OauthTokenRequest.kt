package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.OauthTokenResponse

data class OauthTokenRequest(
    @JsonProperty("client_id")
    val clientId: String,
    @JsonProperty("client_secret")
    val clientSecret: String,
    val code: String,
    @JsonProperty("redirect_uri")
    val redirectUri: String?
) : GithubRequest<OauthTokenResponse>() {
    override fun getHttpMethod() = HttpMethod.POST

    override fun getApiPath() = "login/oauth/access_token"
}

package com.tencent.devops.common.sdk.github

import com.tencent.devops.common.sdk.github.response.GHOauthTokenResponse

interface GithubClient {

    fun <T> execute(oauthToken: String, request: GithubRequest<T>): T

    fun <T> execute(username: String, token: String, request: GithubRequest<T>): T

    fun accessToken(code: String, redirectUri: String? = null): GHOauthTokenResponse

    fun appInstallUrl(
        redirectUri: String,
        state: String
    ): String
}

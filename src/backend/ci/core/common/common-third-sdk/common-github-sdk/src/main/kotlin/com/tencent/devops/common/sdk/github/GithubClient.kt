package com.tencent.devops.common.sdk.github

interface GithubClient {

    fun <T> execute(oauthToken: String, request: GithubRequest<T>): T

    fun <T> execute(username: String, token: String, request: GithubRequest<T>): T

    fun <T> executeByJwt(request: GithubRequest<T>): T
}

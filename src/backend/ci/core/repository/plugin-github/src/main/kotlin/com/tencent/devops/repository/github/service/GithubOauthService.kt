package com.tencent.devops.repository.github.service

import com.tencent.devops.common.sdk.github.AutoRetryGithubClient
import com.tencent.devops.common.sdk.github.DefaultGithubClient
import com.tencent.devops.common.sdk.github.request.GHGetUserRequest
import com.tencent.devops.common.sdk.github.response.GHGetUserResponse
import com.tencent.devops.common.sdk.github.response.GHOauthTokenResponse
import com.tencent.devops.repository.github.config.GithubProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GithubOauthService @Autowired constructor(
    private val defaultGithubClient: DefaultGithubClient,
    private val autoRetryGithubClient: AutoRetryGithubClient,
    private val githubProperties: GithubProperties
) {

    fun getAccessToken(code: String): GHOauthTokenResponse {
        return defaultGithubClient.accessToken(code = code)
    }

    fun getUser(token: String): GHGetUserResponse {
        return autoRetryGithubClient.execute(oauthToken = token, request = GHGetUserRequest())
    }
}

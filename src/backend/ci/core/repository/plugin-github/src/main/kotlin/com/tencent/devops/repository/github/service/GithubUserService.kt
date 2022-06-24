package com.tencent.devops.repository.github.service

import com.tencent.devops.common.sdk.github.DefaultGithubClient
import com.tencent.devops.common.sdk.github.request.GHGetUserRequest
import com.tencent.devops.common.sdk.github.response.GHGetUserResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GithubUserService @Autowired constructor(
    private val defaultGithubClient: DefaultGithubClient
) {

    fun getUser(token: String): GHGetUserResponse {
        return defaultGithubClient.execute(oauthToken = token, request = GHGetUserRequest())
    }
}

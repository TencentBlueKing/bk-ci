package com.tencent.devops.repository.github.service

import com.tencent.devops.common.sdk.github.DefaultGithubClient
import com.tencent.devops.common.sdk.github.request.CreateCheckRunRequest
import com.tencent.devops.common.sdk.github.request.UpdateCheckRunRequest
import com.tencent.devops.common.sdk.github.response.CheckRunResponse
import com.tencent.devops.repository.github.config.GithubProperties
import org.springframework.beans.factory.annotation.Autowired

class GithubCheckService @Autowired constructor(
    private val defaultGithubClient: DefaultGithubClient,
    private val githubProperties: GithubProperties
) {
    fun createCheckRun(
        request: CreateCheckRunRequest,
        token: String
    ): CheckRunResponse {
        return defaultGithubClient.execute(request = request, oauthToken = token)
    }

    fun updateCheckRun(
        request: UpdateCheckRunRequest,
        token: String
    ): CheckRunResponse {
        return defaultGithubClient.execute(request = request, oauthToken = token)
    }
}
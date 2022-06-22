package com.tencent.devops.repository.github.service

import com.tencent.devops.common.sdk.github.DefaultGithubClient
import com.tencent.devops.common.sdk.github.request.GetPullRequestRequest
import com.tencent.devops.common.sdk.github.request.ListPullRequestFileRequest
import com.tencent.devops.common.sdk.github.response.PullRequestFileResponse
import com.tencent.devops.common.sdk.github.response.PullRequestResponse
import com.tencent.devops.repository.github.config.GithubProperties
import org.springframework.beans.factory.annotation.Autowired

class GithubPRService @Autowired constructor(
    private val defaultGithubClient: DefaultGithubClient,
    private val githubProperties: GithubProperties
) {
    fun getPullRequest(
        request: GetPullRequestRequest,
        token: String
    ): PullRequestResponse {
        return defaultGithubClient.execute(request = request, oauthToken = token)
    }

    fun listPullRequestFiles(
        request: ListPullRequestFileRequest,
        token: String
    ): List<PullRequestFileResponse> {
        return defaultGithubClient.execute(request = request, oauthToken = token)
    }
}
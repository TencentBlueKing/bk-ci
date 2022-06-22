package com.tencent.devops.repository.github.service

import com.tencent.devops.common.sdk.github.DefaultGithubClient
import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.ListCommitRequest
import com.tencent.devops.common.sdk.github.response.CommitResponse
import com.tencent.devops.repository.github.config.GithubProperties
import org.springframework.beans.factory.annotation.Autowired

class GithubCommitsService @Autowired constructor(
    private val defaultGithubClient: DefaultGithubClient,
    private val githubProperties: GithubProperties
) {
    fun listCommits(
        request: ListCommitRequest,
        token: String
    ): List<CommitResponse> {
        return defaultGithubClient.execute(request = request, oauthToken = token)
    }

    fun getCommit(
        request: GetCommitRequest,
        token: String
    ): CommitResponse {
        return defaultGithubClient.execute(request = request, oauthToken = token)
    }
}
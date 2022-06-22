package com.tencent.devops.repository.github.service

import com.tencent.devops.common.sdk.github.DefaultGithubClient
import com.tencent.devops.common.sdk.github.request.GetTreeRequest
import com.tencent.devops.common.sdk.github.response.GithubTreeResponse
import com.tencent.devops.repository.github.config.GithubProperties
import org.springframework.beans.factory.annotation.Autowired

class GithubDatabaseService @Autowired constructor(
    private val defaultGithubClient: DefaultGithubClient,
    private val githubProperties: GithubProperties
) {
    fun getTree(
        request: GetTreeRequest,
        token: String
    ): GithubTreeResponse {
        return defaultGithubClient.execute(request = request, oauthToken = token)
    }
}
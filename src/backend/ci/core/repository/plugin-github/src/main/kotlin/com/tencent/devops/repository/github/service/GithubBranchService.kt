package com.tencent.devops.repository.github.service

import com.tencent.devops.common.sdk.github.DefaultGithubClient
import com.tencent.devops.common.sdk.github.request.GHGetBranchRequest
import com.tencent.devops.common.sdk.github.request.GHListBranchesRequest
import com.tencent.devops.common.sdk.github.response.GHBranchResponse
import com.tencent.devops.repository.github.config.GithubProperties
import org.springframework.beans.factory.annotation.Autowired

class GithubBranchService @Autowired constructor(
    private val defaultGithubClient: DefaultGithubClient,
    private val githubProperties: GithubProperties
) {
    fun listBranch(
        request: GHListBranchesRequest,
        token: String
    ): List<GHBranchResponse> {
        return defaultGithubClient.execute(request = request, oauthToken = token)
    }

    fun getBranch(
        request: GHGetBranchRequest,
        token: String
    ): GHBranchResponse {
        return defaultGithubClient.execute(request = request, oauthToken = token)
    }
}
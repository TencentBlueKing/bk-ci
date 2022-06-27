package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.GetPullRequestRequest
import com.tencent.devops.common.sdk.github.request.ListPullRequestFileRequest
import com.tencent.devops.common.sdk.github.response.PullRequestFileResponse
import com.tencent.devops.common.sdk.github.response.PullRequestResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubPRResource
import com.tencent.devops.repository.github.service.GithubPRService
import com.tencent.devops.repository.service.github.GithubTokenService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubPRResourceImpl @Autowired constructor(
    val githubTokenService: GithubTokenService,
    val githubPRService: GithubPRService
) : ServiceGithubPRResource {

    override fun getPullRequest(userId: String, request: GetPullRequestRequest): Result<PullRequestResponse?> {
        return Result(
            githubPRService.getPullRequest(
                request = request,
                token = githubTokenService.getAccessTokenMustExist(userId).accessToken
            )
        )
    }

    override fun listPullRequestFiles(
        userId: String,
        request: ListPullRequestFileRequest
    ): Result<List<PullRequestFileResponse>> {
        return Result(
            githubPRService.listPullRequestFiles(
                request = request,
                token = githubTokenService.getAccessTokenMustExist(userId).accessToken
            )
        )
    }
}

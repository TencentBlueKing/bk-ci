package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.ListCommitRequest
import com.tencent.devops.common.sdk.github.response.CommitResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubCommitsResource
import com.tencent.devops.repository.github.service.GithubCommitsService
import com.tencent.devops.repository.service.github.GithubTokenService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubCommitsResourceImpl @Autowired constructor(
    val githubTokenService: GithubTokenService,
    val githubCommitsService: GithubCommitsService
) : ServiceGithubCommitsResource {

    override fun listCommits(userId: String, request: ListCommitRequest): Result<List<CommitResponse>> {
        return Result(
            githubCommitsService.listCommits(
                request = request,
                token = githubTokenService.getAccessTokenMustExist(userId).accessToken
            )
        )
    }

    override fun getCommit(userId: String, request: GetCommitRequest): Result<CommitResponse?> {
        return Result(
            githubCommitsService.getCommit(
                request = request,
                token = githubTokenService.getAccessTokenMustExist(userId).accessToken
            )
        )
    }
}

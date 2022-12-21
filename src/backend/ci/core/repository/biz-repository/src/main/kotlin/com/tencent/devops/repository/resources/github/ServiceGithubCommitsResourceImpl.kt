package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.CompareTwoCommitsRequest
import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.ListCommitRequest
import com.tencent.devops.common.sdk.github.response.CommitResponse
import com.tencent.devops.common.sdk.github.response.CompareTwoCommitsResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubCommitsResource
import com.tencent.devops.repository.github.service.GithubCommitsService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubCommitsResourceImpl @Autowired constructor(
    val githubCommitsService: GithubCommitsService
) : ServiceGithubCommitsResource {

    override fun listCommits(token: String, request: ListCommitRequest): Result<List<CommitResponse>> {
        return Result(
            githubCommitsService.listCommits(
                request = request,
                token = token
            )
        )
    }

    override fun getCommit(token: String, request: GetCommitRequest): Result<CommitResponse?> {
        return Result(
            githubCommitsService.getCommit(
                request = request,
                token = token
            )
        )
    }

    override fun compareTwoCommits(
        token: String,
        request: CompareTwoCommitsRequest
    ): Result<CompareTwoCommitsResponse?> {
        return Result(
            githubCommitsService.compareTwoCommits(
                token = token,
                request = request
            )
        )
    }
}

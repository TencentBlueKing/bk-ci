package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.GetBranchRequest
import com.tencent.devops.common.sdk.github.request.ListBranchesRequest
import com.tencent.devops.common.sdk.github.response.BranchResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubBranchResource
import com.tencent.devops.repository.github.service.GithubBranchService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubBranchResourceImpl @Autowired constructor(
    val githubBranchService: GithubBranchService
) : ServiceGithubBranchResource {

    override fun listBranch(token: String, request: ListBranchesRequest): Result<List<BranchResponse>> {
        return Result(
            githubBranchService.listBranch(
                request = request,
                token = token
            )
        )
    }

    override fun getBranch(token: String, request: GetBranchRequest): Result<BranchResponse> {
        return Result(
            githubBranchService.getBranch(
                request = request,
                token = token
            )
        )
    }
}

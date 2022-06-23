package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.sdk.github.request.GHGetBranchRequest
import com.tencent.devops.common.sdk.github.request.GHListBranchesRequest
import com.tencent.devops.common.sdk.github.response.GHBranchResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubBranchResource
import com.tencent.devops.repository.github.service.GithubBranchService
import com.tencent.devops.repository.service.github.GithubTokenService
import org.springframework.beans.factory.annotation.Autowired


@RestResource
class ServiceGithubBranchResourceImpl @Autowired constructor(
    val githubTokenService: GithubTokenService,
    val githubBranchService: GithubBranchService
) : ServiceGithubBranchResource {
    
    override fun listBranch(request: GHListBranchesRequest, userId: String): List<GHBranchResponse> {
        return githubBranchService.listBranch(
            request = request,
            token = githubTokenService.getAccessTokenMustExist(userId).accessToken
        )
    }
    
    override fun getBranch(request: GHGetBranchRequest, userId: String): GHBranchResponse {
        return githubBranchService.getBranch(
            request = request,
            token = githubTokenService.getAccessTokenMustExist(userId).accessToken
        )
    }
}

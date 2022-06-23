package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.sdk.github.request.GetTreeRequest
import com.tencent.devops.common.sdk.github.response.GithubTreeResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubDatabaseResource
import com.tencent.devops.repository.github.service.GithubDatabaseService
import com.tencent.devops.repository.service.github.GithubTokenService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubDatabaseResourceImpl @Autowired constructor(
    val githubTokenService: GithubTokenService,
    val githubDatabaseService: GithubDatabaseService
) : ServiceGithubDatabaseResource {

    override fun getTree(userId: String, request: GetTreeRequest): GithubTreeResponse {
        return githubDatabaseService.getTree(
            request = request,
            token = githubTokenService.getAccessTokenMustExist(userId).accessToken
        )
    }
}

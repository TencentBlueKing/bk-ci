package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.GetTreeRequest
import com.tencent.devops.common.sdk.github.response.GetTreeResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubDatabaseResource
import com.tencent.devops.repository.github.service.GithubDatabaseService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubDatabaseResourceImpl @Autowired constructor(
    val githubDatabaseService: GithubDatabaseService
) : ServiceGithubDatabaseResource {

    override fun getTree(token: String, request: GetTreeRequest): Result<GetTreeResponse?> {
        return Result(
            githubDatabaseService.getTree(
                request = request,
                token = token
            )
        )
    }
}

package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.sdk.github.request.CreateCheckRunRequest
import com.tencent.devops.common.sdk.github.request.UpdateCheckRunRequest
import com.tencent.devops.common.sdk.github.response.CheckRunResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubCheckResource
import com.tencent.devops.repository.github.service.GithubCheckService
import com.tencent.devops.repository.service.github.GithubTokenService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubCheckResourceImpl @Autowired constructor(
    val githubTokenService: GithubTokenService,
    val githubCheckService: GithubCheckService
) : ServiceGithubCheckResource {
    
    override fun createCheckRun(request: CreateCheckRunRequest, userId: String): CheckRunResponse {
        return githubCheckService.createCheckRun(
            request = request,
            token = githubTokenService.getAccessTokenMustExist(userId).accessToken
        )
    }
    
    override fun updateCheckRun(request: UpdateCheckRunRequest, userId: String): CheckRunResponse {
        return githubCheckService.updateCheckRun(
            request = request,
            token = githubTokenService.getAccessTokenMustExist(userId).accessToken
        )
    }
}

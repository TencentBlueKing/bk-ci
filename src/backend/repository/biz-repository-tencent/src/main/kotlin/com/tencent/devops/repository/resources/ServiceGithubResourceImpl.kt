package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.repository.service.github.GithubOAuthService
import com.tencent.devops.repository.service.github.GithubService
import com.tencent.devops.repository.service.github.GithubTokenService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubResourceImpl @Autowired constructor(
		private val githubTokenService: GithubTokenService,
		private val githubService: GithubService,
		private val githubOAuthService: GithubOAuthService
) : ServiceGithubResource {
    override fun createAccessToken(
        userId: String,
        accessToken: String,
        tokenType: String,
        scope: String
    ): Result<Boolean> {
        githubTokenService.createAccessToken(userId, accessToken, tokenType, scope)
        return Result(true)
    }

    override fun getAccessToken(userId: String): Result<GithubToken?> {
        return Result(githubTokenService.getAccessToken(userId))
    }
}
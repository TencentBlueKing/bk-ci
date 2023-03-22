package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubOauthResource
import com.tencent.devops.repository.pojo.github.GithubOauthCallback
import com.tencent.devops.repository.pojo.oauth.GithubTokenType
import com.tencent.devops.repository.service.github.GithubOAuthService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubOauthResourceImpl @Autowired constructor(
    private val githubOAuthService: GithubOAuthService
) : ServiceGithubOauthResource {

    override fun githubCallback(code: String, state: String?, channelCode: String?): Result<GithubOauthCallback> {
        return Result(githubOAuthService.githubCallback(code, state, channelCode))
    }

    override fun oauthUrl(redirectUrl: String, userId: String?, tokenType: GithubTokenType?): Result<String> {
        return Result(githubOAuthService.oauthUrl(redirectUrl, userId, tokenType ?: GithubTokenType.GITHUB_APP))
    }
}

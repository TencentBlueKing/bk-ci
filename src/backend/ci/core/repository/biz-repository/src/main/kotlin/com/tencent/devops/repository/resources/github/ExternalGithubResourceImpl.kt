package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ExternalGithubResource
import com.tencent.devops.repository.github.config.GithubProperties
import com.tencent.devops.repository.github.service.GithubOauthService
import com.tencent.devops.repository.service.github.GithubTokenService
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

@RestResource
class ExternalGithubResourceImpl @Autowired constructor(
    private val githubOauthService: GithubOauthService,
    private val githubTokenService: GithubTokenService,
    private val redisOperation: RedisOperation,
    private val githubProperties: GithubProperties
) : ExternalGithubResource {

    companion object {
        private const val BK_LOGIN_GITHUB_CODE_KEY = "bk:login:github:code:%s"
        private val BK_LOGIN_GITHUB_EXPIRED = TimeUnit.MINUTES.toSeconds(10)
    }

    override fun callback(code: String, state: String): Response {
        val oauthTokenResponse = githubOauthService.getAccessToken(code)
        val userResponse = githubOauthService.getUser(oauthTokenResponse.accessToken)
        githubTokenService.createAccessToken(
            userId = userResponse.login,
            accessToken = oauthTokenResponse.accessToken,
            tokenType = oauthTokenResponse.tokenType,
            scope = oauthTokenResponse.scope
        )
        val code = UUIDUtil.generate()
        redisOperation.set(
            key = String.format(BK_LOGIN_GITHUB_CODE_KEY, userResponse.login),
            value = code,
            expiredInSecond = BK_LOGIN_GITHUB_EXPIRED
        )
        val redirectUrl = githubProperties.redirectUrl
        return Response.temporaryRedirect(UriBuilder.fromUri(redirectUrl).build()).build()
    }
}

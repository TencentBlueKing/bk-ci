package com.tencent.devops.external.resources

import com.tencent.devops.external.api.ExternalGithubResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.external.service.github.GithubOauthService
import com.tencent.devops.external.service.github.GithubService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class ExternalGithubResourceImpl @Autowired constructor(
    private val githubService: GithubService,
    private val githubOauthService: GithubOauthService
) : ExternalGithubResource {
    override fun webhookCommit(event: String, guid: String, signature: String, body: String): Result<Boolean> {
        logger.info("Github webhook [event=$event, guid=$guid, signature=$signature, body=$body]")
        githubService.webhookCommit(event, guid, signature, body)
        return Result(true)
    }

    override fun oauthCallback(code: String, state: String): Response {
        logger.info("Github oauth callback [code=$code, state=$state]")
        return githubOauthService.githubCallback(code, state)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
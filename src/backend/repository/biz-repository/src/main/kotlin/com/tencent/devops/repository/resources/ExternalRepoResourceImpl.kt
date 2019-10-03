package com.tencent.devops.repository.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ExternalRepoResource
import com.tencent.devops.repository.service.scm.GitOauthService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class ExternalRepoResourceImpl @Autowired constructor(
    private val gitService: GitOauthService
) : ExternalRepoResource {
    override fun gitCallback(code: String, state: String): Response {
        return gitService.gitCallback(code, state)
    }
}
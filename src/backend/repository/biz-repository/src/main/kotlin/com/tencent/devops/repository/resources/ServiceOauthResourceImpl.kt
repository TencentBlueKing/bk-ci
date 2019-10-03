package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.ServiceOauthResource
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.scm.GitOauthService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceOauthResourceImpl @Autowired constructor(
    private val gitService: GitOauthService
) : ServiceOauthResource {
    override fun gitGet(userId: String): Result<GitToken?> {
        return Result(gitService.getAccessToken(userId))
    }
}
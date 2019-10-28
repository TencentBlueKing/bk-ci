package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.BuildOauthResource
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.scm.GitOauthService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildOauthResourceImpl @Autowired constructor(
    private val gitService: GitOauthService
) : BuildOauthResource {
    override fun gitGet(userId: String): Result<GitToken?> {
        return Result(gitService.getAccessToken(userId))
    }
//
//    override fun gitGetV2(userId: String): Result<GitToken?> {
//        return Result(gitService.getAccessToken(userId))
//    }
}
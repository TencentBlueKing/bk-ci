package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.UserGitResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.service.scm.GitOauthService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGitResourceImpl @Autowired constructor(
    private val gitService: GitOauthService
) : UserGitResource {

    override fun isOAuth(userId: String, redirectUrlType: RedirectUrlTypeEnum?, atomCode: String?): Result<AuthorizeResult> {
        return Result(gitService.isOAuth(userId, redirectUrlType, atomCode))
    }

    override fun deleteToken(userId: String): Result<Int> {
        return Result(gitService.deleteToken(userId))
    }

    override fun getProject(userId: String, projectId: String, repoHashId: String?): Result<AuthorizeResult> {
        return Result(gitService.getProject(userId, projectId, repoHashId))
    }
}
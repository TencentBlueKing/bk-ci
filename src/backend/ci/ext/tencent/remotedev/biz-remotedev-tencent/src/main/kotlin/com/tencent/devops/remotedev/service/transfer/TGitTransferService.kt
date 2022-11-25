package com.tencent.devops.remotedev.service.transfer

import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.pojo.github.GithubAppUrl
import com.tencent.devops.remotedev.service.GitTransferService
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

class TGitTransferService @Autowired constructor(
    private val client: Client,
) : GitTransferService {
    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        return client.get(ServiceOauthResource::class).isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            gitProjectId = gitProjectId,
            refreshToken = false
        )
    }

    override fun getProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GithubAppUrl> {
        return client.get(ServiceGitResource::class).getGitCodeProjectList(
            accessToken = getAndCheckOauthToken(userId).accessToken,
            page = page,
            pageSize = pageSize,
            search = search,
            orderBy = GitCodeProjectsOrder.ACTIVITY,
            sort = GitCodeBranchesSort.DESC,
            owned = owned,
            minAccessLevel = minAccessLevel
        ).data?.map { GithubAppUrl(it.webUrl ?: "") } ?: emptyList()
    }

    private fun getAndCheckOauthToken(
        userId: String
    ): GitToken {
        return client.get(ServiceOauthResource::class).gitGet(userId).data ?: throw OauthForbiddenException(
            message = "用户[$userId]尚未进行OAUTH授权，请先授权。"
        )
    }
}

package com.tencent.devops.repository.resources

import com.tencent.devops.repository.api.UserRepositoryOauthResource
import com.tencent.devops.repository.pojo.oauth.Oauth2Url
import com.tencent.devops.repository.pojo.oauth.OauthTokenVo
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.pojo.RepoOauthRefVo
import com.tencent.devops.repository.pojo.oauth.OauthUserVo
import com.tencent.devops.repository.service.RepositoryOauthService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserRepositoryOauthResourceImpl @Autowired constructor(
    val repositoryOauthService: RepositoryOauthService
) : UserRepositoryOauthResource {
    override fun list(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<OauthTokenVo>> {
        return Result(
            repositoryOauthService.list(
                userId = userId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun relSource(
        userId: String,
        scmCode: String,
        page: Int?,
        pageSize: Int?,
        oauthUserId: String?
    ): Result<Page<RepoOauthRefVo>> {
        val resources = repositoryOauthService.listRepoOauthRef(
            userId = if (oauthUserId.isNullOrBlank()) userId else oauthUserId,
            scmCode = scmCode,
            page = page,
            pageSize = pageSize
        )
        return Result(resources)
    }

    override fun delete(
        userId: String,
        scmCode: String,
        oauthUserId: String
    ): Result<Boolean> {
        repositoryOauthService.delete(
            userId = userId,
            scmCode = scmCode,
            oauthUserId = oauthUserId
        )
        return Result(true)
    }

    override fun reset(
        userId: String,
        scmCode: String,
        redirectUrl: String,
        oauthUserId: String?
    ): Result<Oauth2Url> {
        return Result(
            repositoryOauthService.oauthUrl(
                userId = userId,
                scmCode = scmCode,
                redirectUrl = redirectUrl,
                oauthUserId = (oauthUserId ?: "").ifBlank { userId }
            )
        )
    }

    override fun oauthUserList(userId: String, scmCode: String): Result<List<OauthUserVo>> {
        return Result(
            repositoryOauthService.oauthUserList(
                userId = userId,
                scmCode = scmCode
            )
        )
    }
}
